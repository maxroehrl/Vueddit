import * as app from 'tns-core-modules/application';
import {ad} from 'tns-core-modules/utils/utils';

export default class CustomTabs {
  static slideInRight = ad.resources.getId(':anim/slide_in_right');
  static slideOutLeft = ad.resources.getId(':anim/slide_out_left');
  static slideInLeft = ad.resources.getId(':anim/slide_in_left');
  static slideOutRight = ad.resources.getId(':anim/slide_out_right');
  static backArrowId = ad.resources.getDrawableId('ic_arrow_left_white_48dp');
  static fallback = new saschpe.android.customtabs.WebViewFallback();
  static customTabsHelper = saschpe.android.customtabs.CustomTabsHelper.Companion;

  static openUrl(url) {
    if (!url) {
      return;
    }
    const activity = app.android.startActivity || app.android.foregroundActivity;
    const backArrow = androidx.core.content.ContextCompat.getDrawable(activity, this.backArrowId).getBitmap();
    const customTabsIntent = new androidx.browser.customtabs.CustomTabsIntent.Builder()
        .addDefaultShareMenuItem()
        .setShowTitle(true)
        .setStartAnimations(activity, this.slideInRight, this.slideOutLeft)
        .setExitAnimations(activity, this.slideInLeft, this.slideOutRight)
        .setCloseButtonIcon(backArrow)
        .enableUrlBarHiding()
        .build();
    this.customTabsHelper.addKeepAliveExtra(activity, customTabsIntent.intent);
    const uri = android.net.Uri.parse(url);
    this.customTabsHelper.openCustomTab(activity, customTabsIntent, uri, this.fallback);
  }
}
