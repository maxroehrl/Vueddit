import {Screen} from '@nativescript/core/platform';
import moment from 'moment';

export default class Util {
  static getImage(post) {
    return this.getPreview(post, Screen.mainScreen.widthPixels, {url: ''});
  }

  static getPreview(post, preferredWidth=300, noPreview={url: 'res://ic_comment_text_multiple_outline_white_48dp'}) {
    if (post?.preview?.images?.[0]?.resolutions) {
      const resolutions = post.preview.images[0].resolutions;
      const distArr = resolutions.map((resolution) => Math.abs(resolution.width - preferredWidth));
      return resolutions[distArr.indexOf(Math.min(...distArr))];
    } else if (post.thumbnail && !['self', 'default'].includes(post.thumbnail)) {
      return {url: post.thumbnail, height: post.thumbnail_height, width: post.thumbnail_width};
    } else {
      return noPreview;
    }
  }

  static getAspectFixHeight({height, width}) {
    if (height && width) {
      return (height * Screen.mainScreen.widthPixels / width).toFixed(0) + 'px';
    } else {
      return '0px';
    }
  }

  static getTimeFromNow(unixTime) {
    return moment(unixTime * 1000).fromNow();
  }

  static getUnixTime() {
    return Math.round(new Date().getTime() / 1000);
  }
}
