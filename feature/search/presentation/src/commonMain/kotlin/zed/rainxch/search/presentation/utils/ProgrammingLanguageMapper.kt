package zed.rainxch.search.presentation.utils

import githubstore.feature.search.presentation.generated.resources.Res
import githubstore.feature.search.presentation.generated.resources.language_all
import githubstore.feature.search.presentation.generated.resources.language_c
import githubstore.feature.search.presentation.generated.resources.language_cpp
import githubstore.feature.search.presentation.generated.resources.language_csharp
import githubstore.feature.search.presentation.generated.resources.language_dart
import githubstore.feature.search.presentation.generated.resources.language_go
import githubstore.feature.search.presentation.generated.resources.language_java
import githubstore.feature.search.presentation.generated.resources.language_javascript
import githubstore.feature.search.presentation.generated.resources.language_kotlin
import githubstore.feature.search.presentation.generated.resources.language_php
import githubstore.feature.search.presentation.generated.resources.language_python
import githubstore.feature.search.presentation.generated.resources.language_ruby
import githubstore.feature.search.presentation.generated.resources.language_rust
import githubstore.feature.search.presentation.generated.resources.language_swift
import githubstore.feature.search.presentation.generated.resources.language_typescript
import org.jetbrains.compose.resources.StringResource
import zed.rainxch.domain.model.ProgrammingLanguage
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

fun ProgrammingLanguage.label(): StringResource = when (this) {
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