package br.jss.motoreviso.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import br.jss.motoreviso.fragments.EspecificacoesFragment;
import br.jss.motoreviso.fragments.ManutencoesVeiculoFragment;
import br.jss.motoreviso.fragments.TrajetosVeiculoFragment;
import br.jss.motoreviso.fragments.ImagensVeiculoFragment;

public class DetalheVeiculoTabAdapter extends FragmentStateAdapter {
    private String veiculoId;
    private String tipoVeiculo;

    public DetalheVeiculoTabAdapter(@NonNull FragmentActivity fragmentActivity,
                                    String veiculoId, String tipoVeiculo) {
        super(fragmentActivity);
        this.veiculoId = veiculoId;
        this.tipoVeiculo = tipoVeiculo;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new EspecificacoesFragment(veiculoId);
            case 1:
                return new ManutencoesVeiculoFragment(veiculoId);
            case 2:
                return new TrajetosVeiculoFragment(veiculoId);
            case 3:
                return new ImagensVeiculoFragment(veiculoId);
            default:
                return new EspecificacoesFragment(veiculoId);
        }
    }

    @Override
    public int getItemCount() {
        return 4;
    }
}