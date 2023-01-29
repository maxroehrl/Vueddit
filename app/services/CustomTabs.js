import * as app from '@nativescript/core/application';
import {ad} from '@nativescript/core/utils';

export default class CustomTabs {
  static slideInRight = ad.resources.getId(':anim/slide_in_right');
  static slideOutLeft = ad.resources.getId(':anim/slide_out_left');
  static slideInLeft = ad.resources.getId(':anim/slide_in_left');
  static slideOutRight = ad.resources.getId(':anim/slide_out_right');
  static backArrowId = ad.resources.getDrawableId('ic_arrow_left');
  static fallback = new saschpe.android.customtabs.WebViewFallback();
  static customTabsHelper = saschpe.android.customtabs.CustomTabsHelper.Companion;

  static openUrl(url) {
    if (!url) {
      return;
    }
    const activity = app.android.startActivity || app.android.foregroundActivity;
    const vectorDrawable = androidx.core.content.ContextCompat.getDrawable(activity, this.backArrowId);
    const bitmap = android.graphics.Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), android.graphics.Bitmap.Config.ARGB_8888);
    const canvas = new android.graphics.Canvas(bitmap);
    vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
    vectorDrawable.draw(canvas);
    const customTabsIntent = new androidx.browser.customtabs.CustomTabsIntent.Builder()
        .addDefaultShareMenuItem()
        .setShowTitle(true)
        .setStartAnimations(activity, this.slideInRight, this.slideOutLeft)
        .setExitAnimations(activity, this.slideInLeft, this.slideOutRight)
        .setCloseButtonIcon(bitmap)
        .enableUrlBarHiding()
        .build();
    this.customTabsHelper.addKeepAliveExtra(activity, customTabsIntent.intent);
    const uri = android.net.Uri.parse(url);
    this.customTabsHelper.openCustomTab(activity, customTabsIntent, uri, this.fallback);
  }
}
