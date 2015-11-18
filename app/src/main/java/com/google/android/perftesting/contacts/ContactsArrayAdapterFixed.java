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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.perftesting.R;

import com.bumptech.glide.Glide;

import java.util.List;

/**
 * ContactsArrayAdapter adapts a Contact to a View for use in a ListView.
 */
public class ContactsArrayAdapterFixed extends ArrayAdapter<Contact> {

    public final String TAG = "ContactsArrayAdapter";

    public ContactsArrayAdapterFixed(Context context, List<Contact> contacts) {
        super(context, 0, contacts);
    }

    @ Override
    public View getView(int position, View convertView, ViewGroup parent ) {

        Contact contact = getItem(position);
        LayoutInflater inflater = LayoutInflater.from(getContext());

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_contact, parent, false);
        }

        TextView contactName = (TextView) convertView.findViewById(R.id.contact_name);
        ImageView contactImage = (ImageView) convertView.findViewById(R.id.contact_image);

        Glide.with(contactImage.getContext())
                .load(R.drawable.bbq)
                .fitCenter()
                .into(contactImage);

        contactName.setText(contact.getName());

        return convertView;
    }
}
