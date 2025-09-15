### README_JA.md (日文文档)

# 高機能計算器アプリ

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org)
[![API](https://img.shields.io/badge/API-26%2B-brightgreen.svg)](https://android-arsenal.com/api?level=26)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

インテリジェント百分率計算、自動括弧補完、包括的履歴管理を備えたAndroid向け高機能計算器アプリケーション。

## 主要機能

### 🧮 基本計算機能
- **四則演算**: 加算、減算、乗算、除算
- **小数サポート**: 高精度小数計算
- **括弧計算**: ネスト括弧を含む複雑な式の評価
- **連続演算**: 複数の演算をシームレスに連結

### 🎯 高度な機能
- **インテリジェント百分率**: コンテキスト対応百分率計算
  - 単純: `50% → 0.5`
  - 乗算: `200 × 10% → 20`
  - 加算: `200 + 10% → 220`
  - 複雑: `200 × 50 + 10% → 11000`
  
- **自動括弧補完**: 不足する閉じ括弧を自動補完
- **スマート入力**: 乗算記号の自動挿入、前導ゼロの処理
- **符号切替**: +/-ボタンのインテリジェント動作
- **繰り返し演算**: 連続等号による繰り返し計算

### 📱 ユーザーエクスペリエンス
- **計算履歴**: 過去の計算の保存、表示、再利用
- **自動フォーマット**: 3桁区切りと動的フォントサイズ
- **エラー処理**: 包括的エラー管理（ゼロ除算など）
- **レスポンシブデザイン**: 様々な画面サイズに最適化

## 技術仕様

- **プラットフォーム**: Android 8.0+ (API 26+)
- **言語**: Kotlin 100%
- **アーキテクチャ**: MVVMパターン
- **UIフレームワーク**: Android XMLレイアウト
- **データストレージ**: SharedPreferences
- **テスト**: JUnit + Espresso

## インストール

### 前提条件
- Android Studio Hedgehog 2023.1.1+
- JDK 17+
- Android SDK 26+

### ビルド手順
```bash
git clone https://github.com/yourusername/calculator.git
cd calculator
./gradlew clean build
