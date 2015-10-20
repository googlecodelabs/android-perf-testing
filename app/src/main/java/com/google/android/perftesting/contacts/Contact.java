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

import java.util.ArrayList;
import java.util.List;

/**
 * Contact represents a listing of information for
 * a particular person to be stored in your phone.
 */
public class Contact {
    private String mName;

    public Contact(String name) {
        mName = name;
    }

    public String getName() {
        return mName;
    }

    public static List<Contact> createContactsList(int numContacts) {
        List<Contact> contacts = new ArrayList<>();

        for (int i = 1; i <= numContacts; i++) {
            contacts.add(new Contact("Contact " + i));
        }

        return contacts;
    }
}
