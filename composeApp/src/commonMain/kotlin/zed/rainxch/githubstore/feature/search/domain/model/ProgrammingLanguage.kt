package zed.rainxch.githubstore.feature.search.domain.model;

import githubstore.composeapp.generated.resources.Res
import githubstore.composeapp.generated.resources.language_all
import githubstore.composeapp.generated.resources.language_c
import githubstore.composeapp.generated.resources.language_cpp
import githubstore.composeapp.generated.resources.language_csharp
import githubstore.composeapp.generated.resources.language_dart
import githubstore.composeapp.generated.resources.language_go
import githubstore.composeapp.generated.resources.language_java
import githubstore.composeapp.generated.resources.language_javascript
import githubstore.composeapp.generated.resources.language_kotlin
import githubstore.composeapp.generated.resources.language_php
import githubstore.composeapp.generated.resources.language_python
import githubstore.composeapp.generated.resources.language_ruby
import githubstore.composeapp.generated.resources.language_rust
import githubstore.composeapp.generated.resources.language_swift
import githubstore.composeapp.generated.resources.language_typescript
import org.jetbrains.compose.resources.StringResource

enum class ProgrammingLanguage(val queryValue: String?) {
    All(null),
    Kotlin("kotlin"),
    Java("java"),
    JavaScript("javascript"),
    TypeScript("typescript"),
    Python("python"),
    Swift("swift"),
    Rust("rust"),
    Go("go"),
    CSharp("c#"),
    CPlusPlus("c++"),
    C("c"),
    Dart("dart"),
    Ruby("ruby"),
    PHP("php");

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


    companion object {
        fun fromLanguageString(lang: String?): ProgrammingLanguage {
            if (lang == null) return All
            return entries.find {
                it.queryValue?.equals(lang, ignoreCase = true) == true
            } ?: All
        }
    }
}