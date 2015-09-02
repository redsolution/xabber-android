/*
 * Copyright 2014 Ankush Sachdeva
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package github.ankushsachdeva.emojicon;

import github.ankushsachdeva.emojicon.emoji.Emojicon;
import github.ankushsachdeva.emojicon.emoji.People;

import java.util.Arrays;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridView;

import github.ankushsachdeva.emojicon.R;

/**
 * @author Hieu Rocker (rockerhieu@gmail.com)
 * @author 	Ankush Sachdeva (sankush@yahoo.co.in)
 */
public class EmojiconGridView{
	public View rootView;
	EmojiconsPopup mEmojiconPopup;
    EmojiconRecents mRecents;
    Emojicon[] mData;
    
    public EmojiconGridView(Context context, Emojicon[] emojicons, EmojiconRecents recents, EmojiconsPopup emojiconPopup) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
		mEmojiconPopup = emojiconPopup;
		rootView = inflater.inflate(R.layout.emojicon_grid, null);
		setRecents(recents);
		 GridView gridView = (GridView) rootView.findViewById(R.id.Emoji_GridView);
	        if (emojicons== null) {
	            mData = People.DATA;
	        } else {
	            Object[] o = (Object[]) emojicons;
	            mData = Arrays.asList(o).toArray(new Emojicon[o.length]);
	        }
	        EmojiAdapter mAdapter = new EmojiAdapter(rootView.getContext(), mData);
	        mAdapter.setEmojiClickListener(new OnEmojiconClickedListener() {
				
				@Override
				public void onEmojiconClicked(Emojicon emojicon) {
					if (mEmojiconPopup.onEmojiconClickedListener != null) {
			            mEmojiconPopup.onEmojiconClickedListener.onEmojiconClicked(emojicon);
			        }
			        if (mRecents != null) {
			            mRecents.addRecentEmoji(rootView.getContext(), emojicon);
			        }
				}
			});
	        gridView.setAdapter(mAdapter);
	}
    
	private void setRecents(EmojiconRecents recents) {
        mRecents = recents;
    }

    public interface OnEmojiconClickedListener {
        void onEmojiconClicked(Emojicon emojicon);
    }
    
}
