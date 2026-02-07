package zed.rainxch.search.presentation.utils

import githubstore.feature.search.presentation.generated.resources.Res
import org.jetbrains.compose.resources.StringResource
import zed.rainxch.domain.model.ProgrammingLanguage.All
import zed.rainxch.domain.model.ProgrammingLanguage.C
import zed.rainxch.domain.model.ProgrammingLanguage.CPlusPlus
import zed.rainxch.domain.model.ProgrammingLanguage.CSharp
import zed.rainxch.domain.model.ProgrammingLanguage.Dart
import zed.rainxch.domain.model.ProgrammingLanguage.Go
import zed.rainxch.domain.model.ProgrammingLanguage.Java
import zed.rainxch.domain.model.ProgrammingLanguage.JavaScript
import zed.rainxch.domain.model.ProgrammingLanguage.Kotlin
import zed.rainxch.domain.model.ProgrammingLanguage.PHP
import zed.rainxch.domain.model.ProgrammingLanguage.Python
import zed.rainxch.domain.model.ProgrammingLanguage.Ruby
import zed.rainxch.domain.model.ProgrammingLanguage.Rust
import zed.rainxch.domain.model.ProgrammingLanguage.Swift
import zed.rainxch.domain.model.ProgrammingLanguage.TypeScript

fun label(): StringResource = when (this) {
    All -> Res.string.language_all
    Kotlin -> Res.string.language_kotlin
    Java -> Res.string.language_java
    JavaScript -> Res.string.language_javascript
    TypeScript -> Res.string.language_typescript
    Python -> Res.string.language_python
    Swift -> Res.string.language_swift
    Rust -> Res.string.language_rust
    Go -> Res.string.language_go
    CSharp -> Res.string.language_csharp
    CPlusPlus -> Res.string.language_cpp
    C -> Res.string.language_c
    Dart -> Res.string.language_dart
    Ruby -> Res.string.language_ruby
    PHP -> Res.string.language_php
}