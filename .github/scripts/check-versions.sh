#!/usr/bin/env bash
#
# Decides whether a build of the commit checked out may deploy to GitHub Packages, and fails the build if
# its version breaks the rules of the branches:
#
#   main            collects the changes for the next release and deploys its -SNAPSHOT version.
#   release/x.y.z   prepares a later release at the same time and deploys x.y.z-SNAPSHOT.
#   tag x.y.z       publishes the release x.y.z from a commit on main.
#
# A  On release/x.y.z the version is x.y.z-SNAPSHOT, or the build fails.
# B  A push to main or release/* deploys a -SNAPSHOT version only. Any other version is built and tested,
#    but not deployed; D publishes it.
# C  If main or a release branch not yet merged into main deploys a snapshot of the x.y.z of another of
#    these branches, the build warns, but does not fail.
# D  A tag x.y.z deploys only if it equals the version, the version has no -SNAPSHOT and the tagged commit
#    is on main, or the build fails.
# E  A tag deploys only if none of the dependencies, the transitive ones included, and none of the parents
#    is a -SNAPSHOT version, or the build fails.
#
# It reads GITHUB_REF_TYPE and GITHUB_REF_NAME as GitHub Actions sets them, needs the branches of the
# remote REMOTE (default: origin) fetched with their history, and writes deploy=true or deploy=false to
# GITHUB_OUTPUT.

set -euo pipefail

remote="${REMOTE:-origin}"
ref_type="${GITHUB_REF_TYPE:?GITHUB_REF_TYPE is not set}"
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

release_of() {
	echo "${1%-SNAPSHOT}"
}

is_merged_into_main() {
	git merge-base --is-ancestor "$1" "$remote/main"
}

version="$(version_of)"
echo "Version: $version, $ref_type: $ref_name"
deploy=false

if [ "$ref_type" = tag ]; then
	[ "$ref_name" = "$version" ] || fail "The tag $ref_name does not name the version $version."
	[[ "$version" != *-SNAPSHOT ]] || fail "The tag $ref_name names a -SNAPSHOT version; a release has none."
	is_merged_into_main HEAD || fail "The tag $ref_name is not on main; releases are published from main."
	mvn -B -q org.apache.maven.plugins:maven-enforcer-plugin:3.5.0:enforce -Drules=requireReleaseDeps ||
		fail "The release $version depends on a -SNAPSHOT version."
	deploy=true
elif [ "$ref_name" = main ] || [[ "$ref_name" == release/* ]]; then
	if [[ "$ref_name" == release/* ]]; then
		expected="${ref_name#release/}-SNAPSHOT"
		[ "$version" = "$expected" ] || fail "The branch $ref_name carries $version instead of $expected."
	fi

	if [[ "$version" == *-SNAPSHOT ]]; then
		deploy=true
	else
		echo "::notice::$version is no -SNAPSHOT version, so it is not deployed; a tag $version publishes it."
	fi

	# The branches that deploy a snapshot of their own: main and the release branches not merged into it.
	branches=(main)
	while read -r branch; do
		if ! is_merged_into_main "$remote/$branch"; then
			branches+=("$branch")
		fi
	done < <(git for-each-ref --format='%(refname:lstrip=3)' "refs/remotes/$remote/release/")

	if [ "$deploy" = true ] && [[ " ${branches[*]} " == *" $ref_name "* ]]; then
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
