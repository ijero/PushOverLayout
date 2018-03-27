# PushOverLayout

> 浏览器项目剥离而来。
> 浏览首页上推停靠tab效果。
> 至少需要3个子View。

文档地址：https://ijero.github.io/javadoc/push-over-layout/library/cn.ijero.pushover/-push-over-layout/index.html

效果示例：

<video id="video" controls="" preload="none" width='40%'>
	<source id="mp4" src="resource/video/demo.mp4" />
</video>

引入项目：
> 添加源

	repositories {
	    maven {
	        url  "https://dl.bintray.com/jero/android" 
	    }
	}

> dependency

	implementation 'cn.ijero.pushover:push-over-layout:0.1.0'


使用示例：

> xml
> pol_topParallax：视差变化率（0.1F~1.0F）
> pol_overBackgroundEnable：是否显示覆盖层
> pol_shadowEnable：是否显示阴影

```
app:pol_topParallax="0.8"
app:pol_overBackgroundEnable="true"
app:pol_shadowEnable="true"
```

> java

```
// 在Activity中设置监听
pushOverLayout.listenPushChanged = this@MainActivity

// 切换状态
pushOverLayout.toggle()

// 锁定状态（只锁定手势操作，开关不受限制）
pushOverLayout.lockCurrentState = false

// 获取当前的状态是否是停靠状态,不支持直接设置停靠状态，需要调用toggle()或者snapTo()进行设置
pushOverLayout.isSnapped

```

实现PushOverLayout.OnPushChangedListener接口，并覆盖以下回调方法，便于针对监听实现更多的效果：

```
override fun onPushChanged(offsetPixel: Float, percentage: Float) {
    info {
        "onPushChanged : offsetPixel = $offsetPixel"
    }
}

override fun onSnapOffsetChanged(offsetPixel: Float) {
    info {
        "onSnapOffsetChanged : offsetPixel = $offsetPixel"
    }
}

override fun onTopOffsetChanged(offsetPixel: Float) {
    info {
        "onTopOffsetChanged : offsetPixel = $offsetPixel"
    }
}
override fun onPushStateChanged(state: PushOverLayout.SnapState) {
	info {
		"onPushStateChanged : state = $state"
	}
}
```