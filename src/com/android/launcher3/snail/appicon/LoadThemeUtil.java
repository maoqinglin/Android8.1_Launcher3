
package com.android.launcher3.snail.appicon;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.Log;

import com.android.launcher3.R;
import com.android.launcher3.graphics.IconShapeOverride;

import org.w3c.dom.Text;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class LoadThemeUtil {
    public static final boolean THEME_APP_NEED_DECORATE = true;
    private static final String THEME = "snail_theme.xml";
    private static final String SEPARATOR = "/";
    private static final String THEME_ID_CIRCLE = "icon_circle";
    private static final String THEME_ID_ROUND_RECT = "icon_round_rect";

    public static class ThemeApp {
        String mPackageName;
        String mClassName;
        String mIconId;
        String mThemeId;

        public ThemeApp(String packageName, String className) {
            mPackageName = packageName;
            mClassName = className;
        }

        public ThemeApp(String packageName, String className, String iconId,
                        String themeId) {
            mPackageName = packageName;
            mClassName = className;
            mIconId = iconId;
            mThemeId = themeId;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof ThemeApp)) {
                return false;
            }

            ThemeApp otherApp = (ThemeApp) other;

            return mPackageName.equals(otherApp.mPackageName)
                    && mClassName.equals(otherApp.mClassName);
        }

        @Override
        public int hashCode() {
            int result = 3 * mPackageName.hashCode();
            result = 4 * mClassName.hashCode() + 28;
            return result;
        }

        @Override
        public String toString() {
            return "App [packageName=" + mPackageName + ", className="
                    + mClassName + ", iconid=" + mIconId + ", themeid="
                    + mThemeId + "]";
        }
    }

    public static ArrayList<ThemeApp> loadThemeInfos(Context context) {

        return loadThemeInfos(context, getThemeId(context));
    }

    public static String getThemeId(Context context) {
        String currentIconShape = IconShapeOverride.getAppliedValue(context);
        String[] rect = context.getResources().getStringArray(R.array.icon_shape_override_paths_values);
        if (rect != null && rect.length > 3 && !TextUtils.isEmpty(currentIconShape) && currentIconShape.equals(rect[2])) {
            return THEME_ID_ROUND_RECT;
        }
        return THEME_ID_CIRCLE;
    }

    public static ArrayList<ThemeApp> loadThemeInfos(Context context,
                                                     String themeId) {
        SAXParserFactory saxParserFatory = SAXParserFactory.newInstance();
        SAXParser saxParser;
        SimpleSaxHandler ssh = new SimpleSaxHandler(themeId);

        try {
            saxParser = saxParserFatory.newSAXParser();
            AssetManager am = context.getAssets();
            InputStream inputStream = am.open(themeId + SEPARATOR + THEME);
            saxParser.parse(inputStream, ssh);
            if (inputStream != null) {
                inputStream.close();
            }
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            Log.i("lmq","ParserConfigurationException = "+e);
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.i("lmq","FileNotFoundException = "+e);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return ssh.getApps();
    }

    public static Bitmap getThemeIcon(Context context, ThemeApp app) {
        return getThemeIcon(context, app.mThemeId + SEPARATOR + app.mIconId);
    }

    public static Bitmap getThemeIcon(Context context, String path) {
        AssetManager am = context.getAssets();
        try {
            return BitmapFactory.decodeStream(am.open(path));
        } catch (IOException e) {
            return null;
        }
    }

    public static Bitmap getIconPattern(Context context) {
        String path = getThemeId(context) + SEPARATOR + "much_icon_bg.png";
//        if(MuchConfig.getInstance().isLauncherShortcutNeedBg()){
//            path = THEME_ICON_PATTERN;
//        }else{
//            path = THEME_ICON_PATTERN_TRANSLUCENT;
//        }
        Bitmap bitmap = getThemeIcon(context, path);
        if (bitmap == null) {
            bitmap = BitmapFactory.decodeResource(context.getResources(),
                    R.drawable.snail_icon_bg);
        }
        return bitmap;
    }

    public static Bitmap getIconMask(Context context) {
        String themeIconMask = getThemeId(context) + SEPARATOR + "much_icon_mask.png";
        Bitmap bitmap = getThemeIcon(context, themeIconMask);
        if (bitmap == null) {
            bitmap = BitmapFactory.decodeResource(context.getResources(),
                    R.drawable.snail_icon_mask);
        }
        return bitmap;
    }

    static class SimpleSaxHandler extends DefaultHandler {
        private static final String APPLICATION = "application";
        private static final String PACKAGE_NAME = "packageName";
        private static final String CLASS_NAME = "className";
        private static final String ICON_NAME = "iconName";
        private String mThemeId;

        ArrayList<ThemeApp> apps = new ArrayList<ThemeApp>();

        public SimpleSaxHandler(String themeId) {
            mThemeId = themeId;
        }

        public ArrayList<ThemeApp> getApps() {
            return apps;
        }

        @Override
        public void startDocument() throws SAXException {
        }

        @Override
        public void startElement(String uri, String localName, String qName,
                                 Attributes attributes) throws SAXException {
            if (APPLICATION.equals(localName)) {
                String packageName = attributes.getValue(PACKAGE_NAME);
                String className = attributes.getValue(CLASS_NAME);
                String iconName = attributes.getValue(ICON_NAME);
                apps.add(new ThemeApp(packageName, className, iconName,
                        mThemeId));
            }
        }

        @Override
        public void characters(char[] ch, int start, int length)
                throws SAXException {
        }

        @Override
        public void endDocument() throws SAXException {
        }

        @Override
        public void endElement(String uri, String localName, String qName)
                throws SAXException {

        }
    }
}
