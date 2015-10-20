/*
 * Copyright 2015, Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.perftesting.contacts;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.perftesting.R;

import java.util.List;

/**
 * ContactsArrayAdapter adapts a Contact to a View for use in a ListView.
 */
public class ContactsArrayAdapter extends ArrayAdapter<Contact> {

    public final String TAG = "ContactsArrayAdapter";

    public ContactsArrayAdapter(Context context, List<Contact> contacts) {
        super(context, 0, contacts);
    }

    @ Override
    public View getView(int position, View convertView, ViewGroup parent ) {

        Contact contact = getItem(position);
        LayoutInflater inflater = LayoutInflater.from(getContext());

        // This line is wrong, we're inflating a new view always instead of only if it's null.
        // For demonstration purposes, we will leave this here to show the resulting jank.
        convertView = inflater.inflate(R.layout.item_contact, parent, false);

        TextView contactName = (TextView) convertView.findViewById(R.id.contact_name);
        ImageView contactImage = (ImageView) convertView.findViewById(R.id.contact_image);

        contactName.setText(contact.getName());

        // Let's just create another bitmap when we need one. This makes no attempts to re-use
        // bitmaps that were previously used in rendering past list view elements, causing a large
        // amount of memory to be consumed as you scroll farther down the list.
        Bitmap bm = BitmapFactory.decodeResource(convertView.getResources(), R.drawable.bbq);
        contactImage.setImageBitmap(bm);
        return convertView;
    }
}
