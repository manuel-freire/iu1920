package es.ucm.fdi.iw.control;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.ucm.fdi.iw.LocalData;
import es.ucm.fdi.iw.model.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import java.io.*;
import java.util.List;

/**
 * Rest controller for managing a class.
 * Asking questions, removing them, and voting are the main tasks. 
 * 
 * @author mfreire
 */
@RestController
@RequestMapping("api")
public class ApiController {
	
	private static final Logger log = LogManager.getLogger(ApiController.class);
	
	@Autowired 
	private EntityManager entityManager;
	
	@Autowired
	private IwSocketHandler iwSocketHandler;
	
	@Autowired
	private LocalData localData;

	@ResponseStatus(value=HttpStatus.BAD_REQUEST, reason="Invalid request")  // 401
	public static class ApiException extends RuntimeException {
	     public ApiException(String text, Throwable cause) {
	    	 super(text, cause);
	    	 if (cause != null) {
	    		 log.warn(text, cause);
	    	 } else {
	    		 log.info(text);
	    	 }
	     }
	}

	private Instance getInstance(long apiKey, boolean createIfAbsent) {
		List<Instance> results = entityManager.createQuery(
				"from Instance where key = :apiKey", Instance.class)
				.setParameter("apiKey", apiKey)
				.getResultList();
		Instance i = null;
		if (results.isEmpty()) {
			if (createIfAbsent) {
				i = new Instance();
				// FIXME i.setKey(""+apiKey);
				entityManager.persist(i);
				return i;
			} else {
				throw new IllegalArgumentException("Bad apiKey: " + apiKey);
			}
		} else {
			i = results.get(0);
		}
		return i;
	}

	private User getUser(String eid, long instanceId) {
		List<User> results = entityManager.createQuery(
				"from User where eid = :eid and instance.id = :instanceId", User.class)
				.setParameter("eid", eid)
				.setParameter("instanceId", instanceId)
				.getResultList();
		User u = null;
		if ( ! results.isEmpty()) {
			u = results.get(0);
		}
		return u;
	}

    @PostMapping("/login")
    @Transactional
    public GlobalState addClass(
            @RequestBody User user) throws JsonProcessingException {
        log.info("/login/" + new ObjectMapper().writeValueAsString(user));
        return null;
    }

	@PostMapping("/{token}/addclass")
	@Transactional
	public GlobalState addClass(
			@PathVariable String token,
			@RequestBody EClass data) throws JsonProcessingException {
		log.info(token + "/addclass/" + new ObjectMapper().writeValueAsString(data));
        return null;
	}

    @PostMapping("/{token}/addstudent")
    @Transactional
    public GlobalState addStudent(
            @PathVariable String token,
            @RequestBody EClass data) throws JsonProcessingException {
        log.info(token + "/addstudent/" + new ObjectMapper().writeValueAsString(data));
        return null;
    }

    @PostMapping("/{token}/adduser")
    @Transactional
    public GlobalState addUser(
            @PathVariable String token,
            @RequestBody EClass data) throws JsonProcessingException {
        log.info(token + "/adduser/" + new ObjectMapper().writeValueAsString(data));
        return null;
    }

	@PostMapping("/{token}/rm/{oid}")
	@Transactional
	public GlobalState rm(
	        @PathVariable String token,
            @PathVariable String oid) throws JsonProcessingException {
		log.info(token + "/rm/" + oid);
		return null;
	}

    @PostMapping("/{token}/send")
    @Transactional
    public GlobalState send(
            @PathVariable String token,
            @RequestBody UMessage umessage) throws JsonProcessingException {
        log.info(token + "/send/" + new ObjectMapper().writeValueAsString(umessage));
        return null;
    }

	@PostMapping("/{token}/file/{fileId}")
	public @ResponseBody String uploadFile(
			@PathVariable long token,
			@RequestParam MultipartFile file,
			@PathVariable String fileId,
			HttpServletResponse response) throws IOException {

		log.info(token + "/file (POST), " + fileId);

		String error = "";
        if (file.isEmpty() || fileId.isEmpty()) {
            error = "You failed to upload a file for "
                    + token + "/" + fileId + " because the file or its id was empty.";
        } else if (file.getBytes().length > 100000) {
			error = "Only files <= 100k can be uploaded"
					+ token + "/" + fileId + ": size is " + file.getBytes().length;
		} else {
			File f = localData.getFile("user/" + token,
					fileId.replaceAll("/", "_"));
			try (BufferedOutputStream stream =
						 new BufferedOutputStream(
								 new FileOutputStream(f))) {
				stream.write(file.getBytes());
				return "Uploaded " + token + "/" + fileId +
						" into " + f.getAbsolutePath() + "!";
			} catch (Exception e) {
				error = "Upload failed " + token + "/" + fileId +
						" => " + e.getMessage();
			}
		}
		// exit with error, blame user
		response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		return error;
	}

	/**
	 * @param fileId to retrieve
	 * @return the file, or an 'unknown-user' image
	 */
	@RequestMapping(value="/{token}/file/{fileId}",
			method = RequestMethod.GET,
			produces = MediaType.TEXT_PLAIN_VALUE)
	public void getFile(@PathVariable long apiKey,
						@PathVariable String fileId,
						HttpServletResponse response) {

		log.info(apiKey + "/file (GET), " + fileId);

		File f = localData.getFile("user/" + apiKey,
				fileId.replaceAll("/", "_"));
		try (InputStream in = f.exists() ?
				new BufferedInputStream(new FileInputStream(f)) :
				new BufferedInputStream(this.getClass().getClassLoader()
						.getResourceAsStream("unknown-user.jpg"))) {
			FileCopyUtils.copy(in, response.getOutputStream());
		} catch (IOException ioe) {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND); // 404
			log.info("Error retrieving file: " + f + " -- " + ioe.getMessage());
		}
	}
}
