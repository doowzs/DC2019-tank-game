package com.doowzs.tank;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

public class MainActivity extends AppCompatActivity {
    EditText serverAddrText;
    EditText serverPortText;
    EditText usernameText;
    Button connectButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        serverAddrText = findViewById(R.id.ServerAddrText);
        serverPortText = findViewById(R.id.ServerPortText);
        usernameText = findViewById(R.id.UsernameText);
        connectButton = findViewById(R.id.ConnectButton);

        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, GameActivity.class);
                intent.putExtra("addr", serverAddrText.getText().toString());
                intent.putExtra("port", Integer.valueOf(serverPortText.getText().toString()));
                intent.putExtra("username", usernameText.getText().toString());
                startActivity(intent);
            }
        });
    }
}
