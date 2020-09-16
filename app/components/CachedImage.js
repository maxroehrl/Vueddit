import {Property, View} from '@nativescript/core/ui/core/view';
import {ad, RESOURCE_PREFIX} from '@nativescript/core/utils/utils';

let isInitialized = false;

const srcProperty = new Property({
  name: 'src',
  defaultValue: undefined,
});

class CachedImageBase extends View {}

srcProperty.register(CachedImageBase);

export default class CachedImage extends CachedImageBase {
  createNativeView() {
    if (!isInitialized) {
      com.facebook.drawee.backends.pipeline.Fresco.initialize(this._context);
      isInitialized = true;
    }
    this._android = new com.facebook.drawee.view.SimpleDraweeView(this._context);
    // const placeholderImageId = ad.resources.getDrawableId('ic_comment_text_multiple_outline_white_48dp');
    // const placeholderImage = ad.getApplicationContext().getResources().getDrawable(placeholderImageId);
    // this._android.getHierarchy().setPlaceholderImage(placeholderImage, com.facebook.drawee.drawable.ScalingUtils.ScaleType.CENTER);
    return this._android;
  }

  disposeNativeView() {
    this._android = undefined;
  }

  [srcProperty.getDefault]() {
    return undefined;
  }

  [srcProperty.setNative](value) {
    if (value.startsWith(RESOURCE_PREFIX)) {
      value = 'res:/' + ad.resources.getDrawableId(value.substr(RESOURCE_PREFIX.length));
    }
    this._android.setImageURI(android.net.Uri.parse(value), null);
  }
}
