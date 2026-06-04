# 课表 Android

少儿运动馆教练课表管理 App — 记录出勤、计算课时费工资。

## 功能

- 周课表管理（左右滑动切周）
- 出勤录入（日期驱动，自动列出当天课程）
- 课时费工资计算（按地点/分组统计）
- CSV 导入导出
- 云更新检查

## 薪资规则

| 地点 | 规则 |
|------|------|
| 欧阳修 / 木马森林 | ￥55/节 |
| 超能星球（万达） | 学生 x7 + 助教 x3 |

## 构建

```bash
git clone https://github.com/AY1NG666/kebiao-android.git
cd kebiao-android
echo "sdk.dir=你的Android SDK路径" > local.properties
./gradlew assembleDebug
# APK → app/build/outputs/apk/debug/kebiao-vX.Y.Z.apk
```

## CI

每次 push main 分支自动构建 debug APK，产物在 Actions 页下载。

## 技术栈

Kotlin · Jetpack Compose · Room · Navigation · Material 3

## 许可

MIT
