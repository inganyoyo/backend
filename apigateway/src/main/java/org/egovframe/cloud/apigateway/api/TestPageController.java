package org.egovframe.cloud.apigateway.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * org.egovframe.cloud.apigateway.api.TestPageController
 * <p>
 * 테스트 페이지 컨트롤러
 * 기존 로그인/프로필 기능을 유지하면서 HTTP 요청 테스트 기능 제공
 *
 * @version 1.0
 * @since 2025/07/14
 */
@Slf4j
@RequiredArgsConstructor
@RestController
public class TestPageController {

    /**
     * 테스트 페이지를 반환한다
     *
     * @return String HTML 형태의 테스트 페이지
     */
    @GetMapping(value = "/test", produces = MediaType.TEXT_HTML_VALUE)
    public String testPage() {
        log.info("Test page requested");
        return buildSimpleTestPage();
    }

    /**
     * 한글 테스트용 엔드포인트 - Plain Text 형식으로 응답한다
     *
     * @return String 한글 메시지
     */
    @GetMapping(value = "/hello", produces = "text/plain;charset=UTF-8")
    public String helloKorean() {
        log.info("Korean hello test requested");
        return "안녕하세요! API Gateway에서 보내는 한글 메시지입니다. 🚀";
    }

    /**
     * 한글 테스트용 엔드포인트 - JSON 형식으로 응답한다
     *
     * @return String JSON 형태의 한글 메시지
     */
    @GetMapping(value = "/hello-json", produces = "application/json;charset=UTF-8")
    public String helloKoreanJson() {
        log.info("Korean JSON test requested");
        return "{\"message\":\"안녕하세요!\", \"description\":\"API Gateway 한글 JSON 테스트\", \"status\":\"success\"}";
    }

    /**
     * 간단한 로그인 및 프로필 테스트 페이지를 생성한다
     *
     * @return String HTML 형태의 테스트 페이지
     */
    private String buildSimpleTestPage() {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>")
                .append("<html><head>")
                .append("<meta charset='UTF-8'>")
                .append("<title>Login & Profile Test</title>")
                .append("<style>")
                .append("body{font-family:Arial;max-width:800px;margin:20px auto;padding:20px;}")
                .append(".container{background:#fff;padding:20px;margin:10px 0;border:1px solid #ddd;}")
                .append("input,select,textarea{width:100%;padding:8px;margin:5px 0;}")
                .append("button{padding:10px 15px;margin:5px;background:#007bff;color:white;border:none;cursor:pointer;}")
                .append("button:hover{background:#0056b3;}")
                .append(".success{background:#28a745;}")
                .append(".danger{background:#dc3545;}")
                .append(".response{margin:10px 0;padding:10px;background:#f8f9fa;border:1px solid #dee2e6;white-space:pre-wrap;}")
                .append("</style>")
                .append("</head><body>");

        html.append("<h1>🔐 Login & Profile Test Tool</h1>");

        // 로그인 섹션
        html.append("<div class='container'>")
                .append("<h2>로그인</h2>")
                .append("<input type='text' id='username' placeholder='사용자명' value='user1'>")
                .append("<input type='password' id='password' placeholder='비밀번호' value='user123'>")
                .append("<button class='success' onclick='doLogin()'>로그인</button>")
                .append("<button class='danger' onclick='doLogout()'>로그아웃</button>")
                .append("<button onclick='checkAuth()'>인증 확인</button>")
                .append("<div id='authResult' class='response'></div>")
                .append("</div>");

        // 프로필 섹션
        html.append("<div class='container'>")
                .append("<h2>프로필</h2>")
                .append("<button onclick='getProfile()'>내 프로필</button>")
                .append("<button onclick='getAllUsers()'>전체 사용자</button>")
                .append("<div id='profileResult' class='response'></div>")
                .append("</div>");

        // HTTP 테스트 섹션
        html.append("<div class='container'>")
                .append("<h2>HTTP 테스트</h2>")
                .append("<select id='method'>")
                .append("<option value='GET'>GET</option>")
                .append("<option value='POST'>POST</option>")
                .append("</select>")
                .append("<input type='text' id='url' placeholder='URL' value='/board-service/api/board/hello'>")
                .append("<textarea id='body' placeholder='Request Body (JSON)'></textarea>")
                .append("<button onclick='sendRequest()'>요청 보내기</button>")
                .append("<div id='result' class='response'></div>")
                .append("</div>");

        // JavaScript
        html.append("<script>");
        html.append("async function doLogin(){")
                .append("const username=document.getElementById('username').value;")
                .append("const password=document.getElementById('password').value;")
                .append("try{")
                .append("const response=await fetch('/auth-service/api/auth/login',{")
                .append("method:'POST',headers:{'Content-Type':'application/json'},")
                .append("body:JSON.stringify({username,password})});")
                .append("const data=await response.json();")
                .append("document.getElementById('authResult').textContent=JSON.stringify(data,null,2);")
                .append("}catch(e){")
                .append("document.getElementById('authResult').textContent='Error: '+e.message;")
                .append("}}");

        html.append("async function doLogout(){")
                .append("try{")
                .append("const response=await fetch('/auth-service/api/auth/logout',{method:'POST'});")
                .append("const data=await response.json();")
                .append("document.getElementById('authResult').textContent=JSON.stringify(data,null,2);")
                .append("}catch(e){")
                .append("document.getElementById('authResult').textContent='Error: '+e.message;")
                .append("}}");

        html.append("async function checkAuth(){")
                .append("try{")
                .append("const response=await fetch('/auth-service/api/auth/validate');")
                .append("const data=await response.json();")
                .append("document.getElementById('authResult').textContent=JSON.stringify(data,null,2);")
                .append("}catch(e){")
                .append("document.getElementById('authResult').textContent='Error: '+e.message;")
                .append("}}");

        html.append("async function getProfile(){")
                .append("try{")
                .append("const response=await fetch('/user-service/api/users/profile');")
                .append("const data=await response.json();")
                .append("document.getElementById('profileResult').textContent=JSON.stringify(data,null,2);")
                .append("}catch(e){")
                .append("document.getElementById('profileResult').textContent='Error: '+e.message;")
                .append("}}");

        html.append("async function getAllUsers(){")
                .append("try{")
                .append("const response=await fetch('/user-service/api/users');")
                .append("const data=await response.json();")
                .append("document.getElementById('profileResult').textContent=JSON.stringify(data,null,2);")
                .append("}catch(e){")
                .append("document.getElementById('profileResult').textContent='Error: '+e.message;")
                .append("}}");

        html.append("async function sendRequest(){")
                .append("const method=document.getElementById('method').value;")
                .append("const url=document.getElementById('url').value;")
                .append("const body=document.getElementById('body').value;")
                .append("try{")
                .append("const options={method,headers:{'Content-Type':'application/json'}};")
                .append("if(body && method!=='GET')options.body=body;")
                .append("const response=await fetch(url,options);")
                .append("const data=await response.text();")
                .append("document.getElementById('result').textContent=data;")
                .append("}catch(e){")
                .append("document.getElementById('result').textContent='Error: '+e.message;")
                .append("}}");

        html.append("</script>");
        html.append("</body></html>");

        return html.toString();
    }
}
