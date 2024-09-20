# _OAuth2TokenContext_
+ Là 1 Interface sử dụng để chứa context về việc tạo ra _OAuth2Token_ (là 1 class đại diện cho 1 token value) 
+ Tạo ra _OAuth2Token_ bằng cách sử dụng _OAuth2TokenGenerator_ kết hợp với _OAuth2TokenContext_
+ Chứa các method để lấy thông tin về context tạo ra
+ Các triển khai phổ biến là _DefaultOAuth2TokenContext_, _JwtEncodingContext_, _OAuth2TokenClaimsContext_

# _AbstractBuilder_
+ Là 1 class Builder của OAuth2TokenContext sử dụng để tạo ra OAuth2TokenContext gồm các thông tin để tạo ra OAuth2Token
+ Các method trong _OAuth2TokenContext_ sử dụng để lấy ra các thông tin đã tạo ở _AbstractBuilder_
+ Các triển khai của nó nằm trong các Builder của triển khai của OAuth2TokenContext

# _OAuth2TokenGenerator_
+ Là 1 Function Interface sử dụng để generate ra OAuth2Token (có thể là accessToken, refreshToken,...)
+ generate(OAuth2TokenContext context) truyền vào OAuth2TokenContext để generate ra OAuth2Token
+ Các triển khai phổ biến của nó là _JwtGenerator_, _OAuth2AccessTokenGenerator_, _OAuth2RefreshTokenGenerator_

# _DelegatingOAuth2TokenGenerator_
+ Là 1 triển khai phổ biến nhất của _OAuth2TokenGenerator_
+ Khi sử dụng ta có thể sử dụng nhiều triển khai của _OAuth2TokenGenerator_ 
        
        // có thể add các triển khai của OAuth2TokenGenerator vào để có thể generate token dựa vào từng loại đã add vào
        DelegatingOAuth2TokenGenerator(OAuth2TokenGenerator<? extends OAuth2Token>... tokenGenerators)
