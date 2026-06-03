package br.jss.motoreviso.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import br.jss.motoreviso.R;
import br.jss.motoreviso.activities.CadastroPilotoActivity;
import br.jss.motoreviso.activities.PilotProfileActivity;
import br.jss.motoreviso.adapters.PilotoAdapter;
import br.jss.motoreviso.managers.FirebaseManager;
import br.jss.motoreviso.models.Piloto;

public class PilotosFragment extends Fragment {

    private RecyclerView recyclerView;
    private PilotoAdapter adapter;
    private List<Piloto> pilotos;
    private MaterialButton btnAdicionarPiloto;
    private ProgressBar progressBar;
    private TextView textSubtitulo;
    private LinearLayout layoutVazio;

    private FirebaseManager firebaseManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pilotos, container, false);

        firebaseManager = FirebaseManager.getInstance();
        pilotos = new ArrayList<>();

        inicializarViews(view);
        setupRecyclerView();
        carregarPilotos();

        btnAdicionarPiloto.setOnClickListener(v ->
                startActivity(new Intent(getActivity(), CadastroPilotoActivity.class)));

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        carregarPilotos();
    }

    private void inicializarViews(View view) {
        recyclerView      = view.findViewById(R.id.recycler_pilotos);
        btnAdicionarPiloto = view.findViewById(R.id.btn_adicionar_piloto);
        progressBar       = view.findViewById(R.id.progress_bar_pilotos);
        textSubtitulo     = view.findViewById(R.id.text_subtitulo_pilotos);
        layoutVazio       = view.findViewById(R.id.layout_vazio_pilotos);
    }

    private void setupRecyclerView() {
        adapter = new PilotoAdapter(pilotos, piloto -> {
            Intent intent = new Intent(getActivity(), PilotProfileActivity.class);
            intent.putExtra("PILOTO_ID", piloto.getId());
            startActivity(intent);
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void carregarPilotos() {
        progressBar.setVisibility(View.VISIBLE);
        layoutVazio.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);

        firebaseManager.obterTodosPilotos().addOnCompleteListener(task -> {
            if (!isAdded()) return;
            progressBar.setVisibility(View.GONE);

            pilotos.clear();

            if (task.isSuccessful() && task.getResult() != null) {
                for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                    Piloto piloto = doc.toObject(Piloto.class);
                    if (piloto != null) {
                        piloto.setId(doc.getId());
                        pilotos.add(piloto);
                    }
                }
            }

            adapter.notifyDataSetChanged();
            atualizarEstadoVazio();
        });
    }

    private void atualizarEstadoVazio() {
        int total = pilotos.size();
        if (total == 0) {
            recyclerView.setVisibility(View.GONE);
            layoutVazio.setVisibility(View.VISIBLE);
            textSubtitulo.setText("Nenhum piloto cadastrado");
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            layoutVazio.setVisibility(View.GONE);
            textSubtitulo.setText(total + (total == 1 ? " piloto cadastrado" : " pilotos cadastrados"));
        }
    }
}
