import { Body, Controller, Get, HttpCode, HttpStatus, Post } from '@nestjs/common';
import { AuthService } from './auth.service';
import { UserLoginDto, UserRegisterDto} from './dto/index';

@Controller('auth')
export class AuthController {
  constructor(
    private readonly authService: AuthService
  ) { }
  
  /*
  LOGIN AS USER
  */
  @HttpCode(HttpStatus.OK)
  @Post('login')
  async login(@Body() dto: UserLoginDto): Promise<{ access_token: string }> {
    const user = await this.authService.validateUser(
      dto.name,
      dto.password,
    );
    return this.authService.login(user);
  }

  /*
  CREATE NEW USER
  */
  @HttpCode(HttpStatus.CREATED)
  @Post('register')
  async register(@Body() dto: UserRegisterDto): Promise<{ access_token: string }> {
    //register
    const user = await this.authService.register(dto);
    //with register data, create UserLoginDto
    const user_dto: UserLoginDto = {
      name: user.name,
      password: dto.password
    }
    //immediately login user and return access token (prevents another login after register)
    return this.login(user_dto);
  }
}
