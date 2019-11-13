package es.ucm.fdi.iw.control;

import com.fasterxml.jackson.annotation.JsonView;
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
import java.security.SecureRandom;
import java.util.Base64;
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

	@ResponseStatus(value=HttpStatus.FORBIDDEN, reason="Not authorized")  // 403
	public static class ApiAuthException extends RuntimeException {
		public ApiAuthException(String text) {
			super(text);
			log.info(text);
		}
	}

	private Token resolveTokenOrBail(String tokenKey) {
		List<Token> results = entityManager.createQuery(
				"from Token where key = :key", Token.class)
				.setParameter("key", tokenKey)
				.getResultList();
		if ( ! results.isEmpty()) {
			return results.get(0);
		} else {
			throw new ApiException("Invalid token", null);
		}
	}

	/**
	 * Generates random tokens. From https://stackoverflow.com/a/44227131/15472
	 * @param byteLength
	 * @return
	 */
	public static String generateRandomBase64Token(int byteLength) {
		SecureRandom secureRandom = new SecureRandom();
		byte[] token = new byte[byteLength];
		secureRandom.nextBytes(token);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(token); //base64 encoding
	}
	/**
	 * Requests a token from the system. Provides a user to do so, for which only the
	 * password and uid are looked at
	 * @param loginUser attempting to log in.
	 * @throws JsonProcessingException
	 */
    @PostMapping("/login")
	@JsonView(Views.Public.class)
    @Transactional
    public GlobalState login(
            @RequestBody User loginUser) throws JsonProcessingException {
        log.info("/login/" + new ObjectMapper().writeValueAsString(loginUser));

		List<User> results = entityManager.createQuery(
				"from User where uid = :uid", User.class)
				.setParameter("uid", loginUser.getUid())
				.getResultList();
		// only expecting one, because uid is unique
		User u = results.isEmpty() ? null : results.get(0);

		if (u == null // NOTE: THIS SHOULD CHECK AN *ENCODED* PASSWORD
		              // PLAINTEXT IS A VERY BAD IDEA (outside this demo code)
				|| ! u.getPassword().equals(loginUser.getPassword())) {
			throw new ApiAuthException("Invalid uid or password");
		}

		Token token = new Token();
		token.setUser(u);
		token.setKey(generateRandomBase64Token(16));
		entityManager.persist(token);
		return new GlobalState(token);
    }

	@PostMapping("/{token}/addclass")
	@Transactional
	public GlobalState addClass(
			@PathVariable String token,
			@RequestBody EClass data) throws JsonProcessingException {
		log.info(token + "/addclass/" + new ObjectMapper().writeValueAsString(data));
		Token t = resolveTokenOrBail(token);
		User u = t.getUser();
		if ( ! u.hasRole(User.Role.ADMIN)) {
			throw new ApiException("Only admins can add classes", null);
		}
		EClass ec = new EClass();
		ec.setCid(data.getCid());
		ec.setInstance(u.getInstance());
		entityManager.persist(ec);
        return new GlobalState(t);
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
