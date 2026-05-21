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

import com.google.firebase.firestore.QuerySnapshot;

import br.jss.motoreviso.R;
import br.jss.motoreviso.adapters.ManutencaoAdapter;
import br.jss.motoreviso.managers.FirebaseManager;
import br.jss.motoreviso.models.Manutencao;

import java.util.ArrayList;
import java.util.List;

public class ManutencoesVeiculoFragment extends Fragment {
    private String veiculoId;
    private RecyclerView recyclerView;
    private ManutencaoAdapter adapter;
    private List<Manutencao> manutencoes;
    private ProgressBar progressBar;
    private TextView textVazio;
    private FirebaseManager firebaseManager;

    public ManutencoesVeiculoFragment(String veiculoId) {
        this.veiculoId = veiculoId;
    }

    public ManutencoesVeiculoFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manutencoes_veiculo, container, false);

        recyclerView = view.findViewById(R.id.recycler_manutencoes_veiculo);
        progressBar = view.findViewById(R.id.progress_bar);
        textVazio = view.findViewById(R.id.text_vazio);

        if (veiculoId == null) {
            veiculoId = getArguments() != null ? getArguments().getString("VEICULO_ID") : null;
        }

        firebaseManager = FirebaseManager.getInstance();
        manutencoes = new ArrayList<>();

        setupRecyclerView();

        if (veiculoId != null) {
            carregarManutencoesVeiculo();
        }

        return view;
    }

    private void setupRecyclerView() {
        adapter = new ManutencaoAdapter(manutencoes);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void carregarManutencoesVeiculo() {
        progressBar.setVisibility(View.VISIBLE);

        firebaseManager.obterManutencoesVeiculo(veiculoId)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);

                    if (task.isSuccessful()) {
                        manutencoes.clear();
                        QuerySnapshot snapshot = task.getResult();

                        if (snapshot != null) {
                            for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                                Manutencao manutencao = doc.toObject(Manutencao.class);
                                if (manutencao != null) {
                                    manutencao.setId(doc.getId());
                                    manutencoes.add(manutencao);
                                }
                            }
                        }

                        adapter.notifyDataSetChanged();

                        if (manutencoes.isEmpty()) {
                            textVazio.setVisibility(View.VISIBLE);
                        } else {
                            textVazio.setVisibility(View.GONE);
                        }
                    }
                });
    }
}