package br.jss.motoreviso.fragments;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.QuerySnapshot;

import br.jss.motoreviso.R;
import br.jss.motoreviso.activities.CadastroVeiculoActivity;
import br.jss.motoreviso.activities.DetalheVeiculoActivity;
import br.jss.motoreviso.adapters.VeiculoAdapter;
import br.jss.motoreviso.managers.FirebaseManager;
import br.jss.motoreviso.models.Veiculo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VeiculosFragment extends Fragment implements VeiculoAdapter.OnPrincipalChangedListener {
    private RecyclerView recyclerView;
    private VeiculoAdapter adapter;
    private List<Veiculo> veiculos;
    private List<Veiculo> veiculosFiltrados;
    private MaterialButton btnAdicionarVeiculo;
    private MaterialButton btnFiltroTodas;
    private MaterialButton btnFiltroAtencao;
    private ProgressBar progressBar;
    private TextView textVazioMensagem;
    private TextView textSubtitulo;
    private FirebaseManager firebaseManager;
    private int filtroAtivo = 0; // 0 = Todas, 1 = Requer atenção

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_veiculos, container, false);

        recyclerView = view.findViewById(R.id.recycler_veiculos);
        btnAdicionarVeiculo = view.findViewById(R.id.btn_adicionar_veiculo);
        btnFiltroTodas = view.findViewById(R.id.btn_filtro_todas);
        btnFiltroAtencao = view.findViewById(R.id.btn_filtro_atencao);
        progressBar = view.findViewById(R.id.progress_bar);
        textVazioMensagem = view.findViewById(R.id.text_vazio);
        textSubtitulo = view.findViewById(R.id.text_subtitulo_veiculos);

        firebaseManager = FirebaseManager.getInstance();
        veiculos = new ArrayList<>();
        veiculosFiltrados = new ArrayList<>();

        setupRecyclerView();
        setupFiltros();
        carregarVeiculos();

        btnAdicionarVeiculo.setOnClickListener(v -> abrirCadastroVeiculo());

        return view;
    }

    private void setupFiltros() {
        btnFiltroTodas.setOnClickListener(v -> {
            filtroAtivo = 0;
            atualizarFiltros();
        });

        btnFiltroAtencao.setOnClickListener(v -> {
            filtroAtivo = 1;
            atualizarFiltros();
        });
    }

    private void atualizarFiltros() {
        veiculosFiltrados.clear();

        if (filtroAtivo == 0) {
            veiculosFiltrados.addAll(veiculos);
            btnFiltroTodas.setBackgroundColor(getContext().getColor(R.color.secondary));
            btnFiltroTodas.setTextColor(getContext().getColor(android.R.color.white));
            btnFiltroTodas.setStrokeWidth(0);

            btnFiltroAtencao.setBackgroundColor(getContext().getColor(R.color.surface));
            btnFiltroAtencao.setTextColor(getContext().getColor(R.color.text_secondary));
            btnFiltroAtencao.setStrokeColor(ColorStateList.valueOf(getContext().getColor(R.color.text_tertiary)));
        } else {
            for (Veiculo v : veiculos) {
                if (v.precisaRevisao() != null && v.precisaRevisao()) {
                    veiculosFiltrados.add(v);
                }
            }
            btnFiltroTodas.setBackgroundColor(getContext().getColor(R.color.surface));
            btnFiltroTodas.setTextColor(getContext().getColor(R.color.text_secondary));
            btnFiltroTodas.setStrokeColor(ColorStateList.valueOf(getContext().getColor(R.color.text_tertiary)));

            btnFiltroAtencao.setBackgroundColor(getContext().getColor(R.color.secondary));
            btnFiltroAtencao.setTextColor(getContext().getColor(android.R.color.white));
            btnFiltroAtencao.setStrokeWidth(0);
        }

        adapter = new VeiculoAdapter(veiculosFiltrados, veiculo -> {
            Intent intent = new Intent(getActivity(), DetalheVeiculoActivity.class);
            intent.putExtra("VEICULO_ID", veiculo.getId());
            startActivity(intent);
        });
        adapter.setPrincipalListener(this); // ← necessário: novo adapter precisa do listener
        recyclerView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    private void setupRecyclerView() {
        adapter = new VeiculoAdapter(veiculosFiltrados, veiculo -> {
            Intent intent = new Intent(getActivity(), DetalheVeiculoActivity.class);
            intent.putExtra("VEICULO_ID", veiculo.getId());
            startActivity(intent);
        });
        adapter.setPrincipalListener(this);

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

                atualizarFiltros();
                atualizarSubtitulo();

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

    private void atualizarSubtitulo() {
        if (veiculos.isEmpty()) {
            textSubtitulo.setText("Nenhum veículo");
        } else {
            long kmTotal = 0;
            for (Veiculo v : veiculos) {
                if (v.getKmAtual() != null) {
                    kmTotal += v.getKmAtual();
                }
            }
            String subtitle = String.format(Locale.US, "%d veículos - %,d km totais",
                    veiculos.size(), kmTotal).replace(",", ".");
            textSubtitulo.setText(subtitle);
        }
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

    @Override
    public void onPrincipalChanged(Veiculo veiculoSelecionado) {
        if (veiculoSelecionado.getId() == null) return;

        // Atualizar estado local imediatamente para feedback visual instantâneo
        for (Veiculo v : veiculos) {
            v.setPrincipal(v.getId() != null && v.getId().equals(veiculoSelecionado.getId()));
        }
        adapter.notifyDataSetChanged();

        // Persistir no Firebase atomicamente: desmarca todos, marca o escolhido
        firebaseManager.definirVeiculoPrincipal(veiculoSelecionado.getId(), veiculos)
                .addOnFailureListener(e -> {
                    // Em caso de erro, reverter UI e avisar o usuário
                    android.widget.Toast.makeText(getContext(),
                            "Erro ao definir veículo principal", android.widget.Toast.LENGTH_SHORT).show();
                    carregarVeiculos(); // Recarregar do Firebase para estado consistente
                });
    }
}