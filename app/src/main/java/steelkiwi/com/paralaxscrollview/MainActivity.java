package steelkiwi.com.paralaxscrollview;

import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import steelkiwi.com.paralaxscrollview.manager.ParallaxLayoutManager;
import steelkiwi.com.paralaxscrollview.view.Adapter;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler);
        Adapter adapter = new Adapter(getData());
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new ParallaxLayoutManager());
    }

    private List<Integer> getData() {
        List<Integer> strings = new ArrayList<>();
        strings.add(R.color.q1);
        strings.add(R.color.q2);
        strings.add(R.color.q3);
        strings.add(R.color.q4);
        strings.add(R.color.q5);
        strings.add(R.color.q6);
        strings.add(R.color.q7);
        return strings;
    }
}
