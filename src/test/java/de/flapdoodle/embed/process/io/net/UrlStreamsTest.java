package de.flapdoodle.embed.process.io.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.junit.Test;

import de.flapdoodle.embed.process.io.net.UrlStreams.DownloadCopyListener;
import de.flapdoodle.embed.process.runtime.Network;

public class UrlStreamsTest {

	@Test
	public void downloadShouldBeComplete() throws IOException {
		int httpPort = Network.getFreeServerPort();
		long contentLengt = 2*1024*1024;
		byte[] content = randomFilledByteArray((int) contentLengt);
		
		HttpServers.Listener listener=(uri, method, headers, parms, files) -> {
			if (uri.equals("/download")) {
				return Optional.of(HttpServers.response(200, "text/text", content));
			}
			return Optional.empty();
		};
		
		List<Long> downloadSizes = new ArrayList();
		
		try (HttpServers.Server server = HttpServers.httpServer(httpPort, listener)) {
			URLConnection connection = new URL("http://localhost:"+httpPort+"/download?foo=bar").openConnection();
			
			DownloadCopyListener copyListener=(bytesCopied, downloadContentLength) -> {
				downloadSizes.add(bytesCopied);
				assertEquals("contentLengt", contentLengt, downloadContentLength);
			};
			Path destination = UrlStreams.downloadIntoTempFile(connection, copyListener);
			assertNotNull(destination);
			
			File asFile = destination.toFile();
			assertTrue(asFile.exists());
			assertTrue(asFile.isFile());
			byte[] transferedBytes = Files.readAllBytes(destination);
			assertSameContent(content, transferedBytes);
			
			Files.delete(destination);
		}
		
		List<Long> downloadSizesMatchingFullDownload = downloadSizes.stream()
		 	.filter(l -> l == contentLengt)
		 	.collect(Collectors.toList());
		
		assertEquals(1,downloadSizesMatchingFullDownload.size());
		
		List<Long> downloadSizesBiggerThanContentLength = downloadSizes.stream()
			.filter(l -> l > contentLengt)
			.collect(Collectors.toList());
		
		assertTrue(downloadSizesBiggerThanContentLength.isEmpty());
	}

	private void assertSameContent(byte[] expected, byte[] result) {
		assertEquals("length", expected.length, result.length);
		for (int i=0;i<expected.length;i++) {
			assertEquals("length", expected[i], result[i]);
		}
	}


	private byte[] randomFilledByteArray(int size) {
		byte[] content = new byte[size];
		for (int i=0;i<content.length;i++) {
			content[i]=(byte) ThreadLocalRandom.current().nextInt();
		}
		return content;
	}
}