package com.otognan.driverpete.android;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class EndpointEditorActivity extends ActionBarActivity {

    private boolean isLocationA;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_endpoint_editor);

        this.isLocationA = this.getIntent().getExtras().getBoolean("isLocationA");

        ((EditText)findViewById(R.id.locationLabelEditText)).setText(
                this.getIntent().getExtras().getString("label")
        );

        ((EditText)findViewById(R.id.locationAddressLabelText)).setText(
                this.getIntent().getExtras().getString("address")
        );

    }

    public void onSubmitButton(View view) {
        Intent returnIntent = new Intent();
        returnIntent.putExtra("label", ((EditText)findViewById(R.id.locationLabelEditText)).getText().toString());
        returnIntent.putExtra("address", ((EditText)findViewById(R.id.locationAddressLabelText)).getText().toString());
        returnIntent.putExtra("isLocationA", this.isLocationA);
        setResult(RESULT_OK, returnIntent);
        this.finish();
    }

}
