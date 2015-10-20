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

package com.google.android.perftesting;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.google.android.perftesting.contacts.Contact;
import com.google.android.perftesting.contacts.ContactsRecyclerAdapter;

import java.util.List;

/**
 * RecyclerViewActivity creates a RecyclerView and fills it with Contacts.
 */
public class RecyclerViewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recyclerview);

        // Create a new list of 1000 contacts.
        List<Contact> contacts = Contact.createContactsList(1000);

        RecyclerView contactsView = (RecyclerView) findViewById(R.id.contactList);
        ContactsRecyclerAdapter adapter = new ContactsRecyclerAdapter(contacts);
        contactsView.setAdapter(adapter);
        contactsView.setLayoutManager(new LinearLayoutManager(this));
    }
}
