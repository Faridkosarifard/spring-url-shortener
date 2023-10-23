# Short URL
Generate Short Url with Spring Boot 


# **createShortUrl**

**POST**: http://localhost:8083/url

**RequestBody :**

{
    "url": "URL to want be Shorter"
}

**Response :**

-If the processes was successful return "200 OK" with Body:

{
    "url": "url you want be shorter",
    "key": "yAcmlh"
}

-If the operation was not successful return "406 Not Acceptable" with body:


To load page just use key in url :

http://localhost:8083/yAcmlh

