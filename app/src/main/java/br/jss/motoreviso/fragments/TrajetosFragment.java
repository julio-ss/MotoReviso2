package br.jss.motoreviso.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import br.jss.motoreviso.R;
import br.jss.motoreviso.adapters.TrajetoAdapter;
import br.jss.motoreviso.models.Trajeto;

import java.util.ArrayList;
import java.util.List;

public class TrajetosFragment extends Fragment {
    private RecyclerView recyclerView;
    private TrajetoAdapter adapter;
    private List<Trajeto> trajetos;
    private ProgressBar progressBar;
    private TextView textVazioMensagem;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trajetos, container, false);

        recyclerView = view.findViewById(R.id.recycler_trajetos);
        progressBar = view.findViewById(R.id.progress_bar);
        textVazioMensagem = view.findViewById(R.id.text_vazio);

        trajetos = new ArrayList<>();
        setupRecyclerView();

        return view;
    }

    private void setupRecyclerView() {
        adapter = new TrajetoAdapter(trajetos);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
    }
}