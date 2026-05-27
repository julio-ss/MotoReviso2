package br.jss.motoreviso.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;

import br.jss.motoreviso.R;
import br.jss.motoreviso.activities.CadastroManutencaoActivity;
import br.jss.motoreviso.adapters.ManutencaoAdapter;
import br.jss.motoreviso.managers.FirebaseManager;
import br.jss.motoreviso.models.Manutencao;

import java.util.ArrayList;
import java.util.List;

public class ManutencoesFragment extends Fragment {
    private static final String TAG = "ManutencoesFragment";

    private RecyclerView recyclerView;
    private ManutencaoAdapter adapter;
    private List<Manutencao> manutencoes;
    private Button btnAdicionarManutencao;
    private ProgressBar progressBar;
    private TextView textVazioMensagem;
    private FirebaseManager firebaseManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manutencoes, container, false);

        recyclerView = view.findViewById(R.id.recycler_manutencoes);
        btnAdicionarManutencao = view.findViewById(R.id.btn_adicionar_manutencao);
        progressBar = view.findViewById(R.id.progress_bar);
        textVazioMensagem = view.findViewById(R.id.text_vazio);

        firebaseManager = FirebaseManager.getInstance();
        manutencoes = new ArrayList<>();

        setupRecyclerView();

        btnAdicionarManutencao.setOnClickListener(v -> abrirCadastroManutencao());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        carregarManutencoes();
    }

    private void setupRecyclerView() {
        adapter = new ManutencaoAdapter(manutencoes);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void carregarManutencoes() {
        progressBar.setVisibility(View.VISIBLE);
        textVazioMensagem.setVisibility(View.GONE);

        firebaseManager.obterTodasManutencoes()
                .addOnCompleteListener(task -> {
                    if (!isAdded()) return;
                    progressBar.setVisibility(View.GONE);

                    if (task.isSuccessful() && task.getResult() != null) {
                        manutencoes.clear();
                        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                            Manutencao m = doc.toObject(Manutencao.class);
                            if (m != null) {
                                m.setId(doc.getId());
                                manutencoes.add(m);
                            }
                        }
                        adapter.notifyDataSetChanged();
                        textVazioMensagem.setVisibility(manutencoes.isEmpty() ? View.VISIBLE : View.GONE);
                    } else {
                        Log.e(TAG, "Erro ao carregar manutenções", task.getException());
                        textVazioMensagem.setVisibility(View.VISIBLE);
                        textVazioMensagem.setText(R.string.nenhuma_manutencao);
                    }
                });
    }

    private void abrirCadastroManutencao() {
        Intent intent = new Intent(getActivity(), CadastroManutencaoActivity.class);
        startActivity(intent);
    }
}
