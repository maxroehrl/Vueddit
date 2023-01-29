import {Property, View} from '@nativescript/core/ui/core/view';
import {ad, RESOURCE_PREFIX} from '@nativescript/core/utils';

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
    const progressBarDrawable = new com.facebook.drawee.drawable.ProgressBarDrawable();
    progressBarDrawable.setBackgroundColor(0x30FFFFFF);
    progressBarDrawable.setColor(0x8053ba82);
    this._android.getHierarchy().setProgressBarImage(progressBarDrawable);
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
      const id = ad.resources.getDrawableId(value.substr(RESOURCE_PREFIX.length));
      this._android.setImageResource(id);
    } else {
      this._android.setImageURI(value);
    }
  }
}
