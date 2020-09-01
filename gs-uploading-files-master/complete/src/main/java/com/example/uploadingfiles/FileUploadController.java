package com.example.uploadingfiles;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.uploadingfiles.storage.StorageFileNotFoundException;
import com.example.uploadingfiles.storage.StorageService;

@Controller
public class FileUploadController {

	private final StorageService storageService;

	@Autowired
	public FileUploadController(StorageService storageService) {
		this.storageService = storageService;
	}

	@GetMapping("/")
	public String listUploadedFiles(Model model) throws IOException {
		//в HTML файл сохраняется ссылка на файл
		model.addAttribute("files", storageService.loadAll().map(
				path -> MvcUriComponentsBuilder.fromMethodName(FileUploadController.class,
						"serveFile", path.getFileName().toString()).build().toUri().toString())
				.collect(Collectors.toList()));
		return "uploadForm";
	}

	@GetMapping("/files/{filename:.+}")
	@ResponseBody
	public ResponseEntity<Resource> serveFile(@PathVariable String filename) {

		Resource file = storageService.loadAsResource(filename);
		return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
				"attachment; filename=\"" + file.getFilename() + "\"").body(file);
	}

	@PostMapping("/")
	public String handleFileUpload(@RequestParam("file") MultipartFile file,
			RedirectAttributes redirectAttributes) throws IOException {

		storageService.store(file);
		redirectAttributes.addFlashAttribute("message",
				"Вы загрузили " + file.getOriginalFilename() + "!");
		List<String> content = new ArrayList<>();
		List<String> header = new ArrayList<>();
		List<String> middle = new ArrayList<>();
		List<Integer> middleint = new ArrayList<>();
		int search = 0;
		BufferedReader br = new BufferedReader(new FileReader(Convert(file)));
		//Парсится текст в зависимости от кол-ва # в начале строк
		ParseText(content, header, middle, middleint, search, br);
		redirectAttributes.addFlashAttribute("datas",header.toArray());
		redirectAttributes.addFlashAttribute("contents",content.toArray());
		return "redirect:/";
	}

	private void ParseText(List<String> content, List<String> header, List<String> middle, List<Integer> middleint, int search, BufferedReader br) throws IOException {
		String k;
		while ((k = br.readLine()) != null) {
			for (int i = 0; i < k.length(); i++) {
				if (k.charAt(i) == '#') search++;
				else if (search != 0) {
					middleint.add(search);
					search = 0;
				}
			}
			Collections.addAll(middle, k.split("#"));
			middle.removeAll(Collections.singleton(""));
		}
		for (int i = 0; i < middleint.size(); i++) {
			if (middleint.get(i)==1) header.add(middle.get(i));
			content.add(middle.get(i));
		}
	}

	@ExceptionHandler(StorageFileNotFoundException.class)
	public ResponseEntity<?> handleStorageFileNotFound(StorageFileNotFoundException exc) {
		return ResponseEntity.notFound().build();
	}

	public static File Convert(MultipartFile file) throws IOException {
		File convFile = new File(Objects.requireNonNull(file.getOriginalFilename()));
		convFile.createNewFile();
		FileOutputStream fos = new FileOutputStream(convFile);
		fos.write(file.getBytes());
		fos.close();
		return convFile;
	}

}
