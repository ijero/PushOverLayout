package cn.ijero.pushoverlayoutdemo

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import cn.ijero.pushover.PushOverLayout
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class MainActivity : AppCompatActivity(), PushOverLayout.OnPushChangedListener, AnkoLogger {
    override fun onPushChanged(offsetPixel: Float, percentage: Float) {
        info {
            "onPushChanged : offsetPixel = $offsetPixel , percentage = $percentage"
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mainPushOverLayout.listenPushChanged = this
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        MenuInflater(this).inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_toggle -> {
                mainPushOverLayout.toggle()
            }
            R.id.menu_lock -> {
                if (mainPushOverLayout.isLocked()) {
                    mainPushOverLayout.unlock()
                    item.title = "锁定"
                } else {
                    mainPushOverLayout.lock()
                    item.title = "解锁"
                }
            }
            else -> {
            }
        }
        return super.onOptionsItemSelected(item)
    }

}
