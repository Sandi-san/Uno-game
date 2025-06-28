package com.srebot.uno.assets;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

/** Easy access to assets through consts */
public class AssetDescriptors {
    public static final AssetDescriptor<BitmapFont> UI_FONT =
            new AssetDescriptor<BitmapFont>(AssetPaths.UI_FONT, BitmapFont.class);

    public static final AssetDescriptor<Skin> UI_SKIN =
            new AssetDescriptor<Skin>(AssetPaths.UI_SKIN, Skin.class);

    public static final AssetDescriptor<TextureAtlas> GAMEPLAY =
            new AssetDescriptor<TextureAtlas>(AssetPaths.GAMEPLAY, TextureAtlas.class);

    public static final AssetDescriptor<Sound> PICK_SOUND =
            new AssetDescriptor<Sound>(AssetPaths.PICK_SOUND, Sound.class);
    public static final AssetDescriptor<Sound> SET_SOUND =
            new AssetDescriptor<Sound>(AssetPaths.SET_SOUND, Sound.class);

    public static final AssetDescriptor<Music> MAIN_MUSIC =
            new AssetDescriptor<Music>(AssetPaths.MAIN_MUSIC, Music.class);
    public static final AssetDescriptor<Music> GAME_MUSIC_1 =
            new AssetDescriptor<Music>(AssetPaths.GAME_MUSIC_1, Music.class);
    public static final AssetDescriptor<Music> GAME_MUSIC_2 =
            new AssetDescriptor<Music>(AssetPaths.GAME_MUSIC_2, Music.class);

    private AssetDescriptors() {
    }
}
