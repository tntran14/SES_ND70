package vn.sesgroup.hddt.configuration;

import java.io.File;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import vn.sesgroup.hddt.utils.SystemParams;

@Component
public class TempFileCleaner {
	@Scheduled(fixedRate = 3600000)
	public void cleanOldFiles() {
		File folder = new File(SystemParams.DIR_TMP_SAVE_FILES);
		File[] files = folder.listFiles();
		if (files != null) {
			for (File f : files) {
				long diff = System.currentTimeMillis() - f.lastModified();
				if (diff > 60 * 60 * 1000) {
					f.delete();
//                    boolean deleted = f.delete();
//                    if (deleted) {
//                        System.out.println("Deleted old temp file: " + f.getAbsolutePath());
//                    }
				}
			}
		}
	}
}
