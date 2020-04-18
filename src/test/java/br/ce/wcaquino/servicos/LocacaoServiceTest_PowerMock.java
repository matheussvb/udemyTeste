package br.ce.wcaquino.servicos;

import static br.ce.wcaquino.builders.FilmeBuilder.umFilme;
import static br.ce.wcaquino.builders.LocacaoBuilder.umLocacao;
import static br.ce.wcaquino.builders.UsuarioBuilder.umUsuario;
import static br.ce.wcaquino.matchers.DataDiferencaDiasMatchers.ehHoje;
import static br.ce.wcaquino.matchers.DataDiferencaDiasMatchers.ehHojeComDiferencaDias;
import static br.ce.wcaquino.matchers.MatchersProprios.caiNumaSegunda;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import br.ce.wcaquino.dao.LocacaoDao;
import br.ce.wcaquino.entidades.Filme;
import br.ce.wcaquino.entidades.Locacao;
import br.ce.wcaquino.entidades.Usuario;
import br.ce.wcaquino.exceptions.LocadoraException;
import br.ce.wcaquino.utils.DataUtils;

@RunWith(PowerMockRunner.class)
//@PrepareForTest({LocacaoService.class, DataUtils.class}) // power mock prepara para teste
@PrepareForTest({ LocacaoService.class })
public class LocacaoServiceTest_PowerMock {

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Rule
	public ErrorCollector error = new ErrorCollector();

	@InjectMocks
	private LocacaoService service;

	@Mock
	private SPCService spcService;

	@Mock
	private LocacaoDao dao;

	@Mock
	private EmailService emailService;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		service = PowerMockito.spy(service);

	}
	
	@Test//executa metodo privado diretamente
	public void deveCalcularValorLocacao() throws Exception {
		Usuario usuario = umUsuario().agora();
		List<Filme> filmes = Arrays.asList(umFilme().agora());
		//cenario
		
		//acao
		Double valor = (Double) Whitebox.invokeMethod(service, "calcularValorLocacao", filmes);
		
		//verificacao
		Assert.assertThat(valor, is(4.0));
		
	}

	@Test//moca metodo privado para teste
	public void deveAlugarFilmeSemCalcularValor() throws Exception {
		
		// cenario
		Usuario usuario = umUsuario().agora();
		List<Filme> filmes = Arrays.asList(umFilme().agora());
		
		PowerMockito.doReturn(1.0).when(service, "calcularValorLocacao", filmes);

		// acao
		Locacao locacao = service.alugarFilme(usuario, filmes);

		// verificacao
		Assert.assertThat(locacao.getValor(), is(1.0));
		PowerMockito.verifyPrivate(service).invoke("calcularValorLocacao", filmes);
	}

	@Test
	public void deveProrrogarUmaLocacao() {
		// cenario
		Locacao locacao = umLocacao().agora();

		// acao
		service.prorrogarLocacao(locacao, 3);

		// verificacao
		// Mockito.verify(dao).salvar(Mockito.any(Locacao.class)); // new dentro do
		// metodo

		// argument captor para capturar o que foi passado.
		ArgumentCaptor<Locacao> argCaptor = ArgumentCaptor.forClass(Locacao.class);

		Mockito.verify(dao).salvar(argCaptor.capture());

		Locacao locacaoRetorno = argCaptor.getValue();

		error.checkThat(locacaoRetorno.getValor(), CoreMatchers.is(12.0));
//		Assert.assertThat(locacaoRetorno.getValor(), is(4.0));
		error.checkThat(locacaoRetorno.getDataLocacao(), ehHoje());
		error.checkThat(locacaoRetorno.getDataRetorno(), ehHojeComDiferencaDias(3));
	}

	@Test
	public void deveTratarErroNoSPC() throws Exception {
		// cenario
		Usuario usuario = umUsuario().agora();
		List<Filme> filmes = Arrays.asList(umFilme().agora());

		when(spcService.possuiNegativacao(usuario)).thenThrow(new Exception("Falha catastrófica"));

		exception.expect(LocadoraException.class);
		exception.expectMessage("Problemas com SPC, tente novamente!");

		// acao
		service.alugarFilme(usuario, filmes);

		// verificacao

	}

	@Test
	public void deveEnviarEmailParaLocacoesAtrasadas() {
		// cenario
		Usuario usuario1 = umUsuario().agora();
		Usuario usuario2 = umUsuario().comNome("Usuario em dia").agora();
		Usuario usuario3 = umUsuario().comNome("Outro Atrasado").agora();
		List<Locacao> locacoes = Arrays.asList(umLocacao().comUsuario(usuario1).atrasado().agora(),
				umLocacao().comUsuario(usuario2).agora(), umLocacao().comUsuario(usuario3).atrasado().agora(),
				umLocacao().comUsuario(usuario3).atrasado().agora());
		when(dao.obterLocacoesPendentes()).thenReturn(locacoes);

		// acao
		service.notificarAtrasos();

		// verificacao
		verify(emailService, Mockito.times(3)).notificarAtraso(Mockito.any(Usuario.class));
		verify(emailService).notificarAtraso(usuario1);
		verify(emailService, atLeastOnce()).notificarAtraso(usuario3);
		verify(emailService, Mockito.never()).notificarAtraso(usuario2);
		verifyNoMoreInteractions(emailService);
//		verifyZeroInteractions(spcService);
	}

	@Test
	public void naoDeveAlugarFilmeParaNegativadoSPC() throws Exception {
		// cenario
		Usuario usuario = umUsuario().agora();
		List<Filme> filmes = Arrays.asList(umFilme().agora());

		when(spcService.possuiNegativacao(Mockito.any(Usuario.class))).thenReturn(true);

//		exception.expectMessage("Usuário negativo");
//		exception.expect(LocadoraException.class);

		// acao
		try {
			service.alugarFilme(usuario, filmes);
			// verificacao
			Assert.fail();
		} catch (LocadoraException e) {
			Assert.assertThat(e.getMessage(), is("Usuário negativo"));
		}

//		verify(spcService).possuiNegativacao(usuario);

	}

	@Test
	public void deveAlugarFilme() throws Exception {
		// cenario
//		Calendar calendar = Calendar.getInstance();
//		calendar.set(Calendar.DAY_OF_MONTH, 28);
//		calendar.set(Calendar.MONTH, Calendar.APRIL);
//		calendar.set(Calendar.YEAR, 2017);
//
//		PowerMockito.mockStatic(Calendar.class);
//		PowerMockito.when(Calendar.getInstance()).thenReturn(calendar);
//
//		Usuario usuario = umUsuario().agora();
//		List<Filme> filmes = Arrays.asList(umFilme().comValor(5.0).agora());
//
//		// acao
//		Locacao locacao = service.alugarFilme(usuario, filmes);
//
//		// verificacao
//		error.checkThat(DataUtils.isMesmaData(locacao.getDataLocacao(), DataUtils.obterData(28, 4, 2017)), is(true));
//		error.checkThat(DataUtils.isMesmaData(locacao.getDataRetorno(), DataUtils.obterData(29, 4, 2017)), is(true));
		Usuario usuario = umUsuario().agora();
		List<Filme> filmes = Arrays.asList(umFilme().comValor(5.0).agora());

		PowerMockito.whenNew(Date.class).withNoArguments().thenReturn(DataUtils.obterData(28, 4, 2017));
		
		// acao
		Locacao locacao = service.alugarFilme(usuario, filmes);

		// verificacao
		error.checkThat(DataUtils.isMesmaData(locacao.getDataLocacao(), DataUtils.obterData(28, 4, 2017)), is(true));
		error.checkThat(DataUtils.isMesmaData(locacao.getDataRetorno(), DataUtils.obterData(29, 4, 2017)), is(true));
	}

	@Test
//	@Ignore
	public void deveDevolverNaSegundaAoAlugarNoSabado() throws Exception {
////		Assume.assumeTrue(DataUtils.verificarDiaSemana(new Date(), Calendar.SATURDAY));
//		// cenario
//		Usuario usuario = umUsuario().agora();
//		List<Filme> filmes = Arrays.asList(umFilme().agora());
//
////		PowerMockito.whenNew(Date.class).withNoArguments().thenReturn(DataUtils.obterData(29, 4, 2017));
//		Calendar calendar = Calendar.getInstance();
//		calendar.set(Calendar.DAY_OF_MONTH, 29);
//		calendar.set(Calendar.MONTH, Calendar.APRIL);
//		calendar.set(Calendar.YEAR, 2017);
//
//		PowerMockito.mockStatic(Calendar.class);
//		PowerMockito.when(Calendar.getInstance()).thenReturn(calendar);
//		// acao
//		Locacao retorno = service.alugarFilme(usuario, filmes);
//
//		// verificacao
////		boolean ehSegunda = DataUtils.verificarDiaSemana(retorno.getDataRetorno(), Calendar.MONDAY);
////		Assert.assertTrue(ehSegunda);
////		assertThat(retorno.getDataRetorno(), new DiaSemanaMatcher(Calendar.MONDAY));
////		assertThat(retorno.getDataRetorno(), caiEm(Calendar.MONDAY));
//
////		assertThat(retorno.getDataRetorno(), caiEm(Calendar.MONDAY));
//		assertThat(retorno.getDataRetorno(), caiNumaSegunda());
//
////		PowerMockito.verifyNew(Date.class, Mockito.times(2)).withNoArguments();
//
//		PowerMockito.verifyStatic(Mockito.times(2));
//		Calendar.getInstance();


		
		
		// cenario
		Usuario usuario = umUsuario().agora();
		List<Filme> filmes = Arrays.asList(umFilme().agora());

		PowerMockito.whenNew(Date.class).withNoArguments().thenReturn(DataUtils.obterData(29, 4, 2017));
		// acao
		Locacao retorno = service.alugarFilme(usuario, filmes);

		// verificacao
		assertThat(retorno.getDataRetorno(), caiNumaSegunda());


	}

}
