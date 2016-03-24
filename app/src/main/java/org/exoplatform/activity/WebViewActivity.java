package org.exoplatform.activity;

/*
 * Copyright (C) 2003-2016 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 */

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import org.exoplatform.App;
import org.exoplatform.R;
import org.exoplatform.fragment.PlatformWebViewFragment;
import org.exoplatform.fragment.WebViewFragment;
import org.exoplatform.model.Server;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

/**
 * Created by chautn on 10/14/15. Activity that loads Platform into a web view
 */
public class WebViewActivity extends AppCompatActivity implements PlatformWebViewFragment.PlatformNavigationCallback,
    WebViewFragment.WebViewFragmentCallback {

  public static final String      INTENT_KEY_URL = "URL";

  private static final String     LOG_TAG        = WebViewActivity.class.getName();

  private PlatformWebViewFragment platformFragment;

  private WebViewFragment         webViewFragment;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_webview);
    // toolbar, hidden by default, visible on certain pages cf
    // PlatformWebViewFragment->onPageStarted
    Toolbar mToolbar = (Toolbar) findViewById(R.id.WebClient_Toolbar);
    setSupportActionBar(mToolbar);
    if (getSupportActionBar() != null) {
      getSupportActionBar().hide();
      getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
    // url of the intranet to load
    String url = getIntent().getStringExtra(INTENT_KEY_URL);
    try {
      Server server = new Server(new URL(url), new Date().getTime());
      platformFragment = PlatformWebViewFragment.newInstance(server);
      getSupportFragmentManager().beginTransaction().add(R.id.WebClient_WebViewFragment, platformFragment).commit();
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException("Cannot load the Platform intranet at URL " + url, e);
    }
  }

  @Override
  protected void onStop() {
    super.onStop();
    // Saving the last time an intranet was visited, for rule SIGN_IN_13
    SharedPreferences.Editor pref = getSharedPreferences(App.Preferences.FILE_NAME, 0).edit();
    pref.putLong(App.Preferences.LAST_VISIT_TIME, System.nanoTime());
    pref.apply();
  }

  @Override
  public void onBackPressed() {
    // leave the activity if there is no previous page to go back to,
    // on either Platform fragment or WebView fragment
    boolean eventHandled = false;
    if (platformFragment != null && platformFragment.isVisible())
      eventHandled = platformFragment.goBack();
    else if (webViewFragment != null && webViewFragment.isVisible())
      eventHandled = webViewFragment.goBack();
    if (!eventHandled)
      super.onBackPressed();
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home) {
      finish();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  private void showHideToolbar(boolean show) {
    if (getSupportActionBar() != null) {
      if (show)
        getSupportActionBar().show();
      else
        getSupportActionBar().hide();
    }
  }

  @Override
  public void isOnPageWithoutNavigation(boolean value) {
    showHideToolbar(value);
  }

  @Override
  public void onUserSignedOut() {
    // fragments and activity will be cleaned-up automatically
    finish();
  }

  @Override
  public void onLoadExternalContent(String url) {
    // create and open a new fragment
    webViewFragment = WebViewFragment.newInstance(url);
    getSupportFragmentManager().beginTransaction()
                               .setCustomAnimations(R.anim.fragment_enter_bottom_up, 0, 0, R.anim.fragment_exit_top_down)
                               .add(R.id.WebClient_WebViewFragment, webViewFragment)
                               .addToBackStack("WEBVIEW_FRAGMENT")
                               .hide(platformFragment)
                               .commit();
  }

  @Override
  public void onCloseFragment() {
    // remove the fragment from the activity
    getSupportFragmentManager().popBackStack();
    getSupportFragmentManager().beginTransaction().remove(webViewFragment).show(platformFragment).commit();
    // a new instance will be created if we load an external url again
    webViewFragment = null;
  }
}