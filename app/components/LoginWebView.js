import {WebView} from '@nativescript/core/ui';
import {Property} from '@nativescript/core/ui/core/view';

const shouldOverrideUrlLoadingProperty = new Property({
  name: 'shouldOverrideUrlLoading',
  defaultValue: undefined,
});

shouldOverrideUrlLoadingProperty.register(WebView);

export default class LoginWebView extends WebView {
  callback;

  initNativeView() {
    super.initNativeView();
    this.nativeViewProtected.client.shouldOverrideUrlLoading = (view, url) => this.callback(url);
  }

  [shouldOverrideUrlLoadingProperty.getDefault]() {
    return () => false;
  }

  [shouldOverrideUrlLoadingProperty.setNative](value) {
    this.callback = value;
  }
}
