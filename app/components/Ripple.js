import {ContentView} from '@nativescript/core/ui/content-view';

const rippleColor = android.graphics.Color.parseColor('#53ba82');

export default class Ripple extends ContentView {
  createNativeView() {
    this._android = new com.balysv.materialripple.MaterialRippleLayout(this._context);
    this._android.setRippleOverlay(true);
    this._android.setRippleColor(rippleColor);

    if (!this._androidViewId) {
      this._androidViewId = android.view.View.generateViewId();
    }
    this._android.setId(this._androidViewId);

    return this._android;
  }

  disposeNativeView() {
    this._android = undefined;
  }
}
