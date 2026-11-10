<!-- see https://shields.io/badges -->

![GitHub License](https://img.shields.io/github/license/advantest/markdown-core)
[![Java CI with Maven][ci-badge]][ci-workflow]

[ci-badge]: https://github.com/advantest/markdown-core/actions/workflows/ci-docker.yml/badge.svg?branch=main
[ci-workflow]: https://github.com/advantest/markdown-core/actions/workflows/ci-docker.yml

# markdown-core

*markdown-core* is a tiny library offering a certain version, set of extensions, and configuration of
[flexmark](https://github.com/advantest/flexmark-java), a Markdown parser and HTML generator,
and a certain version of [PlantUML](https://github.com/plantuml/plantuml).
The rationale for markdown-core is using the same combination of libraries and configuration in multiple other tools
and thus, producing the same rendering results.


# PlantUML edition

markdown-core renders diagrams with PlantUML's EPL edition, `net.sourceforge.plantuml:plantuml-epl`, published under
the Eclipse Public License. PlantUML's GPL edition, `net.sourceforge.plantuml:plantuml`, is not an option: the GPL is
incompatible with the BSD 3-Clause License and would put its terms on everything distributed together with it.
The EPL fits as well because ELK, the Eclipse Layout Kernel that PlantUML can lay out diagrams with, is under the EPL
too. The EPL's copyleft covers PlantUML's own files only, so markdown-core keeps its licence.

What follows for an application using markdown-core:

* Do not add the GPL edition. markdown-core takes PlantUML and its version from flexmark-ext-plantuml. To use another
  PlantUML version, override the version of `plantuml-epl` and of Guava, which flexmark-ext-plantuml pins to the one
  PlantUML bundles.
* The PlantUML JAR bundles ELK, EMF, JLaTeXMath and Guava. Maven resolves them a second time from PlantUML's POM, so
  that licence reports and SBOMs list them; a distribution needs only the copies inside the PlantUML JAR.
* The EPL edition renders no Ditaa, JCCKit or Sudoku diagrams and writes no WebP images.


# License

This work is made available under the terms of the BSD 3-Clause License.
The licence accompanies this distribution: [LICENSE](LICENSE).

# Copyright

Copyright (c) 2022-2026, Advantest Europe GmbH.
All rights reserved.