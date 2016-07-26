/*
 * Copyright 2016 Guilherme Penedo
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.guipenedo.pokeradar.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.widget.TextView;

import com.guipenedo.pokeradar.R;

public class AboutActivity extends AppCompatActivity {

    final String html = "<h3>License</h3><p>Copyright 2016 Guilherme Penedo</p><p>Licensed under the Apache License, Version 2.0 (the \"License\"); you may not use this software except in compliance with the License. You may obtain a copy of the License at</p><p><a href=\"http://www.apache.org/licenses/LICENSE-2.0\">http://www.apache.org/licenses/LICENSE-2.0</a></p><p>Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.</p><h3>Source code</h3><p>Available at <a href=\"https://github.com/guipenedo/PokeRadar\">https://github.com/guipenedo/PokeRadar</a>. Feel free to contribute!</p><h3>Third party software</h3><ul><li><a href=\"https://github.com/Grover-c13/PokeGOAPI-Java\">PokeGOAPI-Java</a> - <a href=\"https://github.com/Grover-c13/PokeGOAPI-Java/blob/Development/LICENSE.txt\">License</a></li></ul>";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        TextView htmlTextView = (TextView) findViewById(R.id.aboutText);
        htmlTextView.setText(Html.fromHtml(html));
    }

    @Override
    protected void onStart() {
        super.onStart();
        TextView textView = (TextView) findViewById(R.id.aboutText);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
