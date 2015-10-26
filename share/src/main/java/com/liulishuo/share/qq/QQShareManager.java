package com.liulishuo.share.qq;

import com.liulishuo.share.ShareBlock;
import com.liulishuo.share.base.Constants;
import com.liulishuo.share.base.share.IShareManager;
import com.liulishuo.share.base.share.ShareStateListener;
import com.liulishuo.share.base.shareContent.ShareContent;
import com.liulishuo.share.util.PicFileUtil;
import com.tencent.connect.share.QQShare;
import com.tencent.connect.share.QzoneShare;
import com.tencent.tauth.IUiListener;
import com.tencent.tauth.Tencent;
import com.tencent.tauth.UiError;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by echo on 5/18/15.
 */
public class QQShareManager implements IShareManager{

    private static String KEY_SHARE_TO_FRIEND = "key_share_to_friend";
    
    private static Tencent mTencent;

    private Context mContext;

    private static ShareStateListener mShareStateListener;
    
    public QQShareManager(Context context) {
        String appId = ShareBlock.getInstance().QQAppId;
        mContext = context;
        if (!TextUtils.isEmpty(appId)) {
            mTencent = Tencent.createInstance(appId, mContext.getApplicationContext());
        } else {
            throw new NullPointerException("请通过shareBlock初始化QQAppId");
        }
    }

    public void share(ShareContent shareContent, @ShareBlock.ShareType int shareType, ShareStateListener listener) {
        mShareStateListener = listener;
        Bundle bundle = new Bundle();
        if (shareType == ShareBlock.QQ_FRIEND) {
            shareToQQ(shareContent);
        } else if (shareType == ShareBlock.QQ_ZONE) {
            shareToQZone(bundle, shareContent);
        }
    }

    private void shareToQQ(ShareContent shareContent) {
        Bundle bundle;
        switch (shareContent.getType()) {
            case Constants.SHARE_TYPE_TEXT:
                // 纯文字
                // FIXME: 2015/10/23 文档中说： "本接口支持3种模式，每种模式的参数设置不同"，这三种模式中不包含纯文本
                Toast.makeText(mContext.getApplicationContext(), "QQ目前不支持分享纯文本信息", Toast.LENGTH_SHORT).show();
                bundle = getTextObj();
                return;
            case Constants.SHARE_TYPE_PIC:
                // 纯图片
                bundle = getImageObj(shareContent);
                break;
            case Constants.SHARE_TYPE_WEBPAGE:
                // 网页
                bundle = getWebPageObj();
                break;
            case Constants.SHARE_TYPE_MUSIC:
                // 音乐
                bundle = getMusicObj(shareContent);
                break;
            default:
                throw new UnsupportedOperationException("不支持的分享内容");
        }
        shareMsgToQQFriend(bundle, shareContent);
    }

    /**
     * @see "http://wiki.open.qq.com/wiki/mobile/API%E8%B0%83%E7%94%A8%E8%AF%B4%E6%98%8E#1.13_.E5.88.86.E4.BA.AB.E6.B6.88.E6.81.AF.E5.88.B0QQ.EF.BC.88.E6.97.A0.E9.9C.80QQ.E7.99.BB.E5.BD.95.EF.BC.89"
     *
     * QQShare.PARAM_TITLE 	        必填 	String 	分享的标题, 最长30个字符。
     * QQShare.SHARE_TO_QQ_KEY_TYPE 	必填 	Int 	分享的类型。图文分享(普通分享)填Tencent.SHARE_TO_QQ_TYPE_DEFAULT
     * QQShare.PARAM_TARGET_URL 	必填 	String 	这条分享消息被好友点击后的跳转URL。
     * QQShare.PARAM_SUMMARY 	        可选 	String 	分享的消息摘要，最长40个字。
     * QQShare.SHARE_TO_QQ_IMAGE_URL 	可选 	String 	分享图片的URL或者本地路径
     * QQShare.SHARE_TO_QQ_APP_NAME 	可选 	String 	手Q客户端顶部，替换“返回”按钮文字，如果为空，用返回代替
     * QQShare.SHARE_TO_QQ_EXT_INT 	可选 	Int 	分享额外选项，两种类型可选（默认是不隐藏分享到QZone按钮且不自动打开分享到QZone的对话框）：
     * QQShare.SHARE_TO_QQ_FLAG_QZONE_AUTO_OPEN，分享时自动打开分享到QZone的对话框。
     * QQShare.SHARE_TO_QQ_FLAG_QZONE_ITEM_HIDE，分享时隐藏分享到QZone按钮
     *
     * target必须是真实的可跳转链接才能跳到QQ = =！
     *
     * 发送给QQ好友
     */
    private void shareMsgToQQFriend(Bundle params, ShareContent shareContent) {
        params.putBoolean(KEY_SHARE_TO_FRIEND, true);
        params.putString(QQShare.SHARE_TO_QQ_TITLE, shareContent.getTitle()); // 标题
        params.putString(QQShare.SHARE_TO_QQ_SUMMARY, shareContent.getSummary()); // 描述
        params.putString(QQShare.SHARE_TO_QQ_TARGET_URL, shareContent.getURL()); // 这条分享消息被好友点击后的跳转URL
        if (shareContent.getImageBmpBytes() != null) {
            params.putString(QQShare.SHARE_TO_QQ_IMAGE_URL, PicFileUtil.saveByteArr(shareContent.getImageBmpBytes())); // 分享图片的URL或者本地路径 (可选)
        }
        params.putString(QQShare.SHARE_TO_QQ_APP_NAME, ShareBlock.getInstance().appName); // 手Q客户端顶部，替换“返回”按钮文字，如果为空，用返回代替 (可选)
        mContext.startActivity(new Intent(mContext, QQShareActivity.class).putExtras(params));
    }

    public static void sendShareMsg(Activity activity, Bundle params) {
        if (params.getBoolean(KEY_SHARE_TO_FRIEND)) {
            mTencent.shareToQQ(activity, params, mShareListener);
        } else {
            mTencent.shareToQzone(activity, params, mShareListener);
        }
    }

    private Bundle getTextObj() {
        final Bundle params = new Bundle();
        params.putInt(QQShare.SHARE_TO_QQ_KEY_TYPE, QQShare.SHARE_TO_QQ_TYPE_DEFAULT);
        return params;
    }

    private Bundle getImageObj(ShareContent shareContent) {
        final Bundle params = new Bundle();
        params.putInt(QQShare.SHARE_TO_QQ_KEY_TYPE, QQShare.SHARE_TO_QQ_TYPE_IMAGE); // 标识分享的是纯图片 (必填)
        params.putString(QQShare.SHARE_TO_QQ_IMAGE_LOCAL_URL, PicFileUtil.saveByteArr(shareContent.getImageBmpBytes())); // 信息中的图片 (必填)
        params.putInt(QQShare.SHARE_TO_QQ_EXT_INT, QQShare.SHARE_TO_QQ_FLAG_QZONE_AUTO_OPEN); // (可选)
        return params;
    }

    private Bundle getWebPageObj() {
        final Bundle params = new Bundle();
        params.putInt(QQShare.SHARE_TO_QQ_KEY_TYPE, QQShare.SHARE_TO_QQ_TYPE_DEFAULT);
        return params;
    }

    private Bundle getMusicObj(ShareContent shareContent) {
        final Bundle params = new Bundle();
        params.putInt(QQShare.SHARE_TO_QQ_KEY_TYPE, QQShare.SHARE_TO_QQ_TYPE_AUDIO); // 标识分享的是音乐 (必填)
        params.putString(QQShare.SHARE_TO_QQ_AUDIO_URL, shareContent.getMusicUrl()); //  音乐链接 (必填)
        return params;
    }

    /**
     * 分享到QQ空间（目前支持图文分享）
     *
     * @see "http://wiki.open.qq.com/wiki/Android_API%E8%B0%83%E7%94%A8%E8%AF%B4%E6%98%8E#1.14_.E5.88.86.E4.BA.AB.E5.88.B0QQ.E7.A9.BA.E9.97.B4.EF.BC.88.E6.97.A0.E9.9C.80QQ.E7.99.BB.E5.BD.95.EF.BC.89"
     * QzoneShare.SHARE_TO_QQ_KEY_TYPE 	    选填      Int 	SHARE_TO_QZONE_TYPE_IMAGE_TEXT（图文）
     * QzoneShare.SHARE_TO_QQ_TITLE 	    必填      Int 	分享的标题，最多200个字符。
     * QzoneShare.SHARE_TO_QQ_SUMMARY 	    选填      String 	分享的摘要，最多600字符。
     * QzoneShare.SHARE_TO_QQ_TARGET_URL    必填      String 	需要跳转的链接，URL字符串。
     * QzoneShare.SHARE_TO_QQ_IMAGE_URL     选填      String 	
     *
     * 注意:QZone接口暂不支持发送多张图片的能力，若传入多张图片，则会自动选入第一张图片作为预览图。多图的能力将会在以后支持。
     *
     * 如果分享的图片url是本地的图片地址那么在分享时会显示图片，如果分享的是图片的网址，那么就不会在分享时显示图片
     */
    private void shareToQZone(Bundle params, ShareContent shareContent) {
        /*params.putString(QzoneShare.SHARE_TO_QQ_KEY_TYPE,SHARE_TO_QZONE_TYPE_IMAGE_TEXT );
        params.putString(QzoneShare.SHARE_TO_QQ_TITLE, "标题");//必填
        params.putString(QzoneShare.SHARE_TO_QQ_SUMMARY, "摘要");//选填
        params.putString(QzoneShare.SHARE_TO_QQ_TARGET_URL, "跳转URL");//必填
        params.putStringArrayList(QzoneShare.SHARE_TO_QQ_IMAGE_URL, "图片链接ArrayList");*/
        params.putBoolean(KEY_SHARE_TO_FRIEND, false);
        params.putInt(QzoneShare.SHARE_TO_QZONE_KEY_TYPE, QzoneShare.SHARE_TO_QZONE_TYPE_IMAGE_TEXT);
        String title = shareContent.getTitle();
        if (title == null) {
            // 如果没title，说明就是分享的纯文字、纯图片
            Toast.makeText(mContext.getApplicationContext(), "QQ空间目前只支持分享图文信息", Toast.LENGTH_SHORT).show();
            return;
        }
        params.putString(QzoneShare.SHARE_TO_QQ_TITLE, title); // 标题
        params.putString(QzoneShare.SHARE_TO_QQ_SUMMARY, shareContent.getSummary()); // 描述
        params.putString(QzoneShare.SHARE_TO_QQ_TARGET_URL, shareContent.getURL()); // 点击后跳转的url

        // 分享的图片, 以ArrayList<String>的类型传入，以便支持多张图片 （注：图片最多支持9张图片，多余的图片会被丢弃）。
        if (shareContent.getImageBmpBytes() != null) {
            ArrayList<String> imageUrls = new ArrayList<>(); // 图片的ArrayList
            imageUrls.add(PicFileUtil.saveByteArr(shareContent.getImageBmpBytes()));
            params.putStringArrayList(QzoneShare.SHARE_TO_QQ_IMAGE_URL, imageUrls);
        }
        mContext.startActivity(new Intent(mContext, QQShareActivity.class).putExtras(params));
    }

    private static final IUiListener mShareListener = new IUiListener() {

        @Override
        public void onComplete(Object response) {
            mShareStateListener.onSuccess();
        }

        @Override
        public void onCancel() {
            mShareStateListener.onCancel();
        }

        @Override
        public void onError(UiError e) {
            mShareStateListener.onError(e.errorCode + " - " + e.errorMessage + " - " + e.errorDetail);
        }
    };

    public static void handlerOnActivityResult(Intent data) {
        if (mShareListener != null) {
            Tencent.handleResultData(data, mShareListener);
        }
    }

}
