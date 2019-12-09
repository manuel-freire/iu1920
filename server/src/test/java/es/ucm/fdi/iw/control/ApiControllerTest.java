package es.ucm.fdi.iw.control;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.JsonPath;
import es.ucm.fdi.iw.IwApplication;

import es.ucm.fdi.iw.model.Message;
import es.ucm.fdi.iw.model.UMessage;
import es.ucm.fdi.iw.model.User;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Random;

import static es.ucm.fdi.iw.control.ApiController.generateRandomBase64Token;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = IwApplication.class)
public class ApiControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private WebApplicationContext wac;
	private MockMvc mockMvc;
	private ObjectMapper om;
	private Random rng;

	@Autowired
	private EntityManager entityManager;



	@Before
	public void setup() throws Exception {
	    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
        MvcResult initializeResult = this.mockMvc.perform(
            get("/api/initialize"))
                .andReturn();
        this.om = new ObjectMapper();
        this.rng = new Random();
	}

	@Test
	public void simpleBadLogin() throws Exception {
	    MvcResult mvcResult = this.mockMvc.perform(
	            post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content("{\"uid\": \"x\"}"))
	      .andDo(print()).andExpect(status().is4xxClientError())
	      .andExpect(content().string(containsString("Expected to find")))
	      .andReturn();
	}

    @Test
    public void simpleGoodLogin() throws Exception {
        MvcResult mvcResult = this.mockMvc.perform(
                post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content("{\"uid\": \"admin_g01\",\"password\": \"!magic\"}"))
                .andDo(print()).andExpect(status().isOk())
                .andReturn();
        String result = mvcResult.getResponse().getContentAsString();
        // see https://github.com/json-path/JsonPath
        String token = JsonPath.parse(result).read("$.token");
        assertTrue(token.length() > 0);
    }

    private String login() throws Exception {
        MvcResult mvcResult = this.mockMvc.perform(
                post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content("{\"uid\": \"admin_g01\",\"password\": \"!magic\"}"))
                .andDo(print()).andExpect(status().isOk())
                .andReturn();
        String result = mvcResult.getResponse().getContentAsString();
        String token = JsonPath.parse(result).read("$.token");
        assertTrue(token.length() > 0);
        return token;
    }

    private void addTelsToArray(ArrayNode a, int n) {
	    for (int i=0; i<n; i++) {
	        StringBuilder sb = new StringBuilder();
	        for (int j=0; j<9; j++) sb.append("" + rng.nextInt(10));
	        sb.insert(3, '-');
	        sb.insert(7, '-');
	        a.add(sb.toString());
        }
    }

    private ObjectNode newRandomUser(ObjectMapper om, String name, User.Role role) {
	    ObjectNode root = om.createObjectNode();
	    root.put("uid", generateRandomBase64Token(5));
	    root.put("first_name", name);
	    root.put("last_name", name + "ez" + " Ejemplez");
	    root.put("type", "" + role);
        addTelsToArray(root.putArray("tels"),
                rng.nextInt(2)+1);
        root.put("password", generateRandomBase64Token(4)
                + "Az"
                + rng.nextInt(10));
        return root;
    }

    private ObjectNode newRandomStudent(ObjectMapper om, String name, String className) {
        ObjectNode root = om.createObjectNode();
        root.put("sid", generateRandomBase64Token(5));
        root.put("first_name", name);
        root.put("last_name", name + "ez" + " Ejemplez");
        root.put("cid", className);
        return root;
    }

    @Test
    public void messageFieldsAreSerialized() throws Exception {
        this.mockMvc.perform(
                get("/api/msg/1")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(om.createObjectNode().put(
                                "id", 1).toString()))
                .andDo(print()).andExpect(status().isOk())
                .andReturn();
    }

    @Test
    public void addSomePeople() throws Exception {
        String token = login();
        ObjectMapper om = new ObjectMapper();

        for (int i=0; i<3; i++) {

            // add a class
            String className = "class_" + (i+1);
            this.mockMvc.perform(
                post("/api/" + token + "/addclass")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(om.createObjectNode().put(
                                 "cid", className).toString()))
                .andDo(print()).andExpect(status().isOk())
                .andReturn();

            // and a teacher for the class
            ObjectNode randomTeacher = newRandomUser(
                    om, "Teacher_" + i, User.Role.TEACHER);
            randomTeacher.putArray("classes").add(className);
            this.mockMvc.perform(
                    post("/api/" + token + "/adduser")
                            .contentType(MediaType.APPLICATION_JSON_UTF8)
                            .content(randomTeacher.toString()))
                    .andDo(print()).andExpect(status().isOk())
                    .andReturn();

            // and 10-20 students
            ArrayList<String> studentIds = new ArrayList<>();
            int studentCount = rng.nextInt(20) + 10;
            while (studentCount --> 0) {
                ObjectNode randomStudent = newRandomStudent(
                        om, "Student_" + i + "_" + studentCount, className);
                studentIds.add(randomStudent.get("sid").asText());
                this.mockMvc.perform(
                        post("/api/" + token + "/addstudent")
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content(randomStudent.toString()))
                        .andDo(print()).andExpect(status().isOk())
                        .andReturn();
            }

            // and some guardians for good measure
            ArrayList<String> guardianIds = new ArrayList<>();
            int guardianCount = rng.nextInt(20) + 10;
            while (studentCount --> 0) {
                ObjectNode randomGuardian = newRandomStudent(
                        om, "Guardian" + i, className);
                studentIds.add(randomGuardian.get("uid").asText());
                this.mockMvc.perform(
                        post("/api/" + token + "/adduser")
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content(randomGuardian.toString()))
                        .andDo(print()).andExpect(status().isOk())
                        .andReturn();
            }

            this.mockMvc.perform(
                    post("/api/" + token + "/adduser")
                            .contentType(MediaType.APPLICATION_JSON_UTF8)
                            .content(newRandomUser(
                                    om, "Guardian_" + i, User.Role.GUARDIAN).toString()))
                    .andDo(print()).andExpect(status().isOk())
                    .andReturn();
        }
	}
}
