

import org.junit.Assert;

import br.ce.wcaquino.entidades.Usuario;

public class AssertTest {

	@org.junit.Test
	public void Test() {

		Assert.assertTrue(true);
		Assert.assertFalse(false);
		Assert.assertEquals("Erro de comparação", 2, 2);
		Assert.assertEquals(0.510, 0.5103, 0.001); // margem de 0.001 casas

		int i = 5;
		Integer i2 = 5;
		Assert.assertEquals(Integer.valueOf(i), i2);
		Assert.assertEquals(i, i2.intValue());

		Assert.assertEquals("bola", "bola");
		Assert.assertNotEquals("bolaa", "bola");
		Assert.assertTrue("bola".equalsIgnoreCase("Bola"));
		Assert.assertTrue("bola".startsWith("bo"));

		Usuario u1 = new Usuario("Usuario 1");
		Usuario u2 = new Usuario("Usuario 1");
		Usuario u3 = null;

		Assert.assertEquals(u1, u2); // hashcode equals

		Assert.assertSame(u2, u2); // mesmo tipo u2 - u2
		Assert.assertNotSame(u1, u2);

		Assert.assertNull(u3);
		Assert.assertNotNull(u1);
		
		
		
		
		
		
		
		
		

	}

}
