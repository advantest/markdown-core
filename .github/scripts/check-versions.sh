#!/usr/bin/env bash
#
# Decides whether a build of the commit checked out may deploy to GitHub Packages, and fails the build if
# its version breaks the rules of the branches. A version is either work in progress, x.y.z-SNAPSHOT, or a
# dated release, x.y.z-yyyyMMdd-HHmm, set in a version commit of its own:
#
#   main            collects the changes for the next release and deploys its versions.
#   release/x.y.z   prepares a later release at the same time and deploys its versions of x.y.z.
#
# A final release is a dated release that is kept; it is never built again. A tag only marks it and starts
# no build.
#
# A  The version is x.y.z-SNAPSHOT or x.y.z-yyyyMMdd-HHmm, on release/x.y.z with that x.y.z, or the build
#    fails.
# B  A push to main or release/* deploys its version. Deploying a dated release a second time fails, as
#    GitHub Packages refuses it.
# C  If main or a release branch not yet merged into main carries the x.y.z of another of these branches,
#    the build warns, but does not fail.
# D  A merge commit on main carrying the dated release of the commit it merges is a final release: it is
#    built and tested, but not deployed, and it does not warn.
# E  A dated release deploys only if none of the dependencies, the transitive ones included, and none of
#    the parents is a -SNAPSHOT version, or the build fails.
#
# It reads GITHUB_REF_NAME as GitHub Actions sets it, needs the branches of the remote REMOTE (default:
# origin) fetched with their history, and writes deploy=true or deploy=false to GITHUB_OUTPUT.

set -euo pipefail

remote="${REMOTE:-origin}"
ref_name="${GITHUB_REF_NAME:?GITHUB_REF_NAME is not set}"

fail() {
	echo "::error::$*"
	exit 1
}

# The version of the project in the pom.xml of a revision, or of the working tree if none is given.
version_of() {
	local pom=pom.xml
	if [ -n "${1:-}" ]; then
		pom="$(mktemp -d)/pom.xml"
		git show "$1:pom.xml" >"$pom"
	fi
	mvn -B -q -N -f "$pom" org.apache.maven.plugins:maven-help-plugin:3.5.1:evaluate \
		-Dexpression=project.version -DforceStdout
}

# The x.y.z of a version.
release_of() {
	echo "${1%%-*}"
}

is_merged_into_main() {
	git merge-base --is-ancestor "$1" "$remote/main"
}

# Whether HEAD is a merge commit and one of the commits it merges carries the version.
merges_version() {
	local parent
	for parent in $(git rev-list --parents -n 1 HEAD | cut -d ' ' -f 3-); do
		if [ "$(version_of "$parent")" = "$1" ]; then
			return 0
		fi
	done
	return 1
}

version="$(version_of)"
echo "Version: $version, branch: $ref_name"
deploy=false

if [[ "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+-SNAPSHOT$ ]]; then
	dated=false
elif [[ "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+-[0-9]{8}-[0-9]{4}$ ]]; then
	dated=true
else
	fail "The version $version is neither x.y.z-SNAPSHOT nor a dated release x.y.z-yyyyMMdd-HHmm."
fi

if [[ "$ref_name" == release/* ]] && [ "$(release_of "$version")" != "${ref_name#release/}" ]; then
	fail "The branch $ref_name carries $version, a version of another release."
fi

if [ "$ref_name" = main ] || [[ "$ref_name" == release/* ]]; then
	deploy=true

	if [ "$ref_name" = main ] && [ "$dated" = true ] && merges_version "$version"; then
		echo "$version is the final release merged into main; it has been deployed already."
		deploy=false
	fi

	if [ "$deploy" = true ] && [ "$dated" = true ]; then
		mvn -B -q org.apache.maven.plugins:maven-enforcer-plugin:3.5.0:enforce -Drules=requireReleaseDeps ||
			fail "The dated release $version depends on a -SNAPSHOT version."
	fi
fi

if [ "$deploy" = true ]; then
	# The branches that deploy versions of their own: main and the release branches not merged into it.
	branches=(main)
	while read -r branch; do
		if ! is_merged_into_main "$remote/$branch"; then
			branches+=("$branch")
		fi
	done < <(git for-each-ref --format='%(refname:lstrip=3)' "refs/remotes/$remote/release/")

	if [[ " ${branches[*]} " == *" $ref_name "* ]]; then
		for branch in "${branches[@]}"; do
			if [ "$branch" != "$ref_name" ]; then
				other="$(version_of "$remote/$branch")"
				if [ "$(release_of "$other")" = "$(release_of "$version")" ]; then
					echo "::warning::$ref_name and $branch both carry $(release_of "$version") and overwrite" \
						"each other's snapshots."
				fi
			fi
		done
	fi
fi

echo "Deploy: $deploy"
echo "deploy=$deploy" >>"${GITHUB_OUTPUT:-/dev/null}"
