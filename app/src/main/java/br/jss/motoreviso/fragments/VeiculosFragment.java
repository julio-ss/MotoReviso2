package br.jss.motoreviso.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.QuerySnapshot;

import br.jss.motoreviso.R;
import br.jss.motoreviso.activities.CadastroVeiculoActivity;
import br.jss.motoreviso.activities.DetalheVeiculoActivity;
import br.jss.motoreviso.adapters.VeiculoAdapter;
import br.jss.motoreviso.managers.FirebaseManager;
import br.jss.motoreviso.models.Veiculo;

import java.util.ArrayList;
import java.util.List;

public class VeiculosFragment extends Fragment {
    private RecyclerView recyclerView;
    private VeiculoAdapter adapter;
    private List<Veiculo> veiculos;
    private Button btnAdicionarVeiculo;
    private ProgressBar progressBar;
    private TextView textVazioMensagem;
    private FirebaseManager firebaseManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_veiculos, container, false);

        recyclerView = view.findViewById(R.id.recycler_veiculos);
        btnAdicionarVeiculo = view.findViewById(R.id.btn_adicionar_veiculo);
        progressBar = view.findViewById(R.id.progress_bar);
        textVazioMensagem = view.findViewById(R.id.text_vazio);

        firebaseManager = FirebaseManager.getInstance();
        veiculos = new ArrayList<>();

        setupRecyclerView();
        carregarVeiculos();

        btnAdicionarVeiculo.setOnClickListener(v -> abrirCadastroVeiculo());

        return view;
    }

    private void setupRecyclerView() {
        adapter = new VeiculoAdapter(veiculos, veiculo -> {
            Intent intent = new Intent(getActivity(), DetalheVeiculoActivity.class);
            intent.putExtra("VEICULO_ID", veiculo.getId());
            startActivity(intent);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void carregarVeiculos() {
        progressBar.setVisibility(View.VISIBLE);

        firebaseManager.obterTodosVeiculos().addOnCompleteListener(task -> {
            progressBar.setVisibility(View.GONE);

            if (task.isSuccessful()) {
                veiculos.clear();
                QuerySnapshot snapshot = task.getResult();

                if (snapshot != null) {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                        Veiculo veiculo = doc.toObject(Veiculo.class);
                        if (veiculo != null) {
                            veiculo.setId(doc.getId());
                            veiculos.add(veiculo);
                        }
                    }
                }

                adapter.notifyDataSetChanged();

                if (veiculos.isEmpty()) {
                    textVazioMensagem.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    textVazioMensagem.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            } else {
                textVazioMensagem.setText("Erro ao carregar veículos");
                textVazioMensagem.setVisibility(View.VISIBLE);
            }
        });
    }

    private void abrirCadastroVeiculo() {
        Intent intent = new Intent(getActivity(), CadastroVeiculoActivity.class);
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        carregarVeiculos();
    }
}