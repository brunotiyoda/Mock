package br.com.caelum.leilao.servico;

import br.com.caelum.leilao.builder.CriadorDeLeilao;
import br.com.caelum.leilao.dominio.Leilao;
import br.com.caelum.leilao.infra.dao.RepositorioDeLeiloes;
import org.junit.Test;
import org.mockito.InOrder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class EncerradorDeLeilaoTest {

    @Test
    public void deveEncerrarLeiloesQueComecaramUmaSemanaAtras() {
        Calendar antiga = Calendar.getInstance();
        antiga.set(2019, 2, 10);

        Leilao leilao1 = new CriadorDeLeilao()
                .para("Computador")
                .naData(antiga)
                .constroi();
        Leilao leilao2 = new CriadorDeLeilao()
                .para("Geladeira")
                .naData(antiga)
                .constroi();

        List<Leilao> leiloesAntigos = Arrays.asList(leilao1, leilao2);

        RepositorioDeLeiloes daoFalso = mock(RepositorioDeLeiloes.class);
        EnviadorDeEmail enviadorDeEmail = mock(EnviadorDeEmail.class);

        when(daoFalso.correntes()).thenReturn(leiloesAntigos);

        EncerradorDeLeilao encerrador = new EncerradorDeLeilao(daoFalso, enviadorDeEmail);
        encerrador.encerra();


        assertEquals(2, encerrador.getTotalEncerrados());
        assertTrue(leilao1.isEncerrado());
        assertTrue(leilao2.isEncerrado());
    }

    @Test
    public void naoDeveEncerrarLeilaoQueComecouOntem() {
        Calendar ontem = Calendar.getInstance();
        ontem.add(Calendar.DAY_OF_MONTH, -1);

        Leilao leilao1 = new CriadorDeLeilao()
                .para("Computador")
                .naData(ontem)
                .constroi();
        Leilao leilao2 = new CriadorDeLeilao()
                .para("Geladeira")
                .naData(ontem)
                .constroi();

        List<Leilao> leiloesNaoEncerrados = Arrays.asList(leilao1, leilao2);

        RepositorioDeLeiloes daoFalso = mock(RepositorioDeLeiloes.class);
        EnviadorDeEmail enviadorDeEmail = mock(EnviadorDeEmail.class);

        when(daoFalso.correntes()).thenReturn(leiloesNaoEncerrados);

        EncerradorDeLeilao encerrador = new EncerradorDeLeilao(daoFalso, enviadorDeEmail);
        encerrador.encerra();

        assertEquals(0, encerrador.getTotalEncerrados());
        assertTrue(!leilao1.isEncerrado());
        assertTrue(!leilao2.isEncerrado());
    }

    @Test
    public void casoNaoHajaNenhumLeilao() {
        RepositorioDeLeiloes daoFalso = mock(RepositorioDeLeiloes.class);
        EnviadorDeEmail enviadorDeEmail = mock(EnviadorDeEmail.class);

        when(daoFalso.correntes()).thenReturn(new ArrayList<Leilao>());

        EncerradorDeLeilao encerrador = new EncerradorDeLeilao(daoFalso, enviadorDeEmail);
        encerrador.encerra();

        assertEquals(0, encerrador.getTotalEncerrados());
    }

    @Test
    public void deveAtulizarLeiloesEncerrados() {
        Calendar antiga = Calendar.getInstance();
        antiga.set(2019, 2, 10);

        Leilao leilao1 = new CriadorDeLeilao()
                .para("Computador")
                .naData(antiga)
                .constroi();

        RepositorioDeLeiloes daoFalso = mock(RepositorioDeLeiloes.class);
        EnviadorDeEmail enviadorDeEmail = mock(EnviadorDeEmail.class);

        when(daoFalso.correntes()).thenReturn(Arrays.asList(leilao1));

        EncerradorDeLeilao encerrador = new EncerradorDeLeilao(daoFalso, enviadorDeEmail);
        encerrador.encerra();

        verify(daoFalso, times(1)).atualiza(leilao1);
    }

    @Test
    public void deveEnviarEmailAposPersistirLeilaoEncerrado() {
        Calendar antiga = Calendar.getInstance();
        antiga.set(1999, 1, 20);

        Leilao leilao1 = new CriadorDeLeilao().para("TV de plasma")
                .naData(antiga).constroi();

        RepositorioDeLeiloes daoFalso = mock(RepositorioDeLeiloes.class);
        when(daoFalso.correntes()).thenReturn(Arrays.asList(leilao1));

        EnviadorDeEmail carteiroFalso = mock(EnviadorDeEmail.class);
        EncerradorDeLeilao encerrador = new EncerradorDeLeilao(daoFalso, carteiroFalso);

        encerrador.encerra();

        InOrder inOrder = inOrder(daoFalso, carteiroFalso);
        inOrder.verify(daoFalso, times(1)).atualiza(leilao1);
        inOrder.verify(carteiroFalso, times(1)).envia(leilao1);
    }

    @Test
    public void deveContinuarAExecucaoMesmoQuandoDaoFalha() {
        Calendar antiga = Calendar.getInstance();
        antiga.set(1999, 1, 20);

        Leilao leilao1 = new CriadorDeLeilao().para("TV de plasma")
                .naData(antiga).constroi();
        Leilao leilao2 = new CriadorDeLeilao().para("Geladeira")
                .naData(antiga).constroi();

        RepositorioDeLeiloes daoFalso = mock(RepositorioDeLeiloes.class);
        when(daoFalso.correntes()).thenReturn(Arrays.asList(leilao1, leilao2));

        doThrow(new RuntimeException()).when(daoFalso).atualiza(leilao1);

        EnviadorDeEmail carteiroFalso = mock(EnviadorDeEmail.class);
        EncerradorDeLeilao encerrador =
                new EncerradorDeLeilao(daoFalso, carteiroFalso);

        encerrador.encerra();

        verify(daoFalso).atualiza(leilao2);
        verify(carteiroFalso).envia(leilao2);
    }

    @Test
    public void deveContinuarAExecucaoMesmoQuandoEnviadorDeEmaillFalha() {
        Calendar antiga = Calendar.getInstance();
        antiga.set(1999, 1, 20);

        Leilao leilao1 = new CriadorDeLeilao().para("TV de plasma")
                .naData(antiga).constroi();
        Leilao leilao2 = new CriadorDeLeilao().para("Geladeira")
                .naData(antiga).constroi();

        RepositorioDeLeiloes daoFalso = mock(RepositorioDeLeiloes.class);
        when(daoFalso.correntes()).thenReturn(Arrays.asList(leilao1, leilao2));

        EnviadorDeEmail carteiroFalso = mock(EnviadorDeEmail.class);
        doThrow(new RuntimeException()).when(carteiroFalso).envia(leilao1);

        EncerradorDeLeilao encerrador =
                new EncerradorDeLeilao(daoFalso, carteiroFalso);

        encerrador.encerra();

        verify(daoFalso).atualiza(leilao2);
        verify(carteiroFalso).envia(leilao2);
    }

    @Test
    public void deveDesistirSeDaoFalhaPraSempre() {
        Calendar antiga = Calendar.getInstance();
        antiga.set(1999, 1, 20);

        Leilao leilao1 = new CriadorDeLeilao().para("TV de plasma")
                .naData(antiga).constroi();
        Leilao leilao2 = new CriadorDeLeilao().para("Geladeira")
                .naData(antiga).constroi();

        RepositorioDeLeiloes daoFalso = mock(RepositorioDeLeiloes.class);
        when(daoFalso.correntes()).thenReturn(Arrays.asList(leilao1, leilao2));

        EnviadorDeEmail carteiroFalso = mock(EnviadorDeEmail.class);
        doThrow(new RuntimeException()).when(daoFalso).atualiza(any(Leilao.class));

        EncerradorDeLeilao encerrador =
                new EncerradorDeLeilao(daoFalso, carteiroFalso);

        encerrador.encerra();

        verify(carteiroFalso, never()).envia(any(Leilao.class));
    }
}
