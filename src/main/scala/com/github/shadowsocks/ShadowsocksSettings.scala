package com.github.shadowsocks

import java.util.Locale

import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.{DialogInterface, Intent, SharedPreferences}
import android.net.Uri
import android.os.{Build, Bundle}
import android.preference.{Preference, PreferenceFragment, SwitchPreference}
import android.support.v7.app.AlertDialog
import android.webkit.{WebView, WebViewClient}
import com.github.shadowsocks.utils.Key

// TODO: Move related logic here
class ShadowsocksSettings extends PreferenceFragment with OnSharedPreferenceChangeListener {
  private lazy val activity = getActivity.asInstanceOf[Shadowsocks]

  private var isProxyApps: SwitchPreference = _

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    addPreferencesFromResource(R.xml.pref_all)
    getPreferenceManager.getSharedPreferences.registerOnSharedPreferenceChangeListener(this)

    isProxyApps = findPreference(Key.isProxyApps).asInstanceOf[SwitchPreference]
    isProxyApps.setOnPreferenceChangeListener((preference: Preference, newValue: Any) => {
      startActivity(new Intent(activity, classOf[AppManager]))
      newValue.asInstanceOf[Boolean]  // keep it ON
    })

    findPreference("recovery").setOnPreferenceClickListener((preference: Preference) => {
      ShadowsocksApplication.track(Shadowsocks.TAG, "reset")
      activity.recovery()
      true
    })

    val flush = findPreference("flush_dnscache")
    if (Build.VERSION.SDK_INT >= 17 && !ShadowsocksApplication.isRoot) {
      flush.setEnabled(false)
      flush.setSummary(R.string.flush_dnscache_summary_disabled)
    } else flush.setOnPreferenceClickListener((preference: Preference) => {
      ShadowsocksApplication.track(Shadowsocks.TAG, "flush_dnscache")
      activity.flushDnsCache()
      true
    })

    findPreference("about").setOnPreferenceClickListener((preference: Preference) => {
      ShadowsocksApplication.track(Shadowsocks.TAG, "about")
      val web = new WebView(activity)
      web.loadUrl("file:///android_asset/pages/about.html")
      web.setWebViewClient(new WebViewClient() {
        override def shouldOverrideUrlLoading(view: WebView, url: String): Boolean = {
          startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)))
          true
        }
      })

      new AlertDialog.Builder(activity)
        .setTitle(getString(R.string.about_title).formatLocal(Locale.ENGLISH, ShadowsocksApplication.getVersionName))
        .setCancelable(false)
        .setNegativeButton(getString(android.R.string.ok),
          ((dialog: DialogInterface, id: Int) => dialog.cancel()): DialogInterface.OnClickListener)
        .setView(web)
        .create()
        .show()
      true
    })
  }

  override def onResume = {
    super.onResume
    isProxyApps.setChecked(ShadowsocksApplication.settings.getBoolean(Key.isProxyApps, false))  // update
  }

  def onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) = key match {
    case Key.isNAT => if (ShadowsocksApplication.isRoot) activity.handler.post(() => {
      activity.deattachService()
      activity.attachService()
    })
    case _ =>
  }
}
