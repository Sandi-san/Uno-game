import { IsString, MinLength } from 'class-validator';

export class UserLoginDto {
  @IsString()
  name: string;

  @IsString()
  @MinLength(6)
  password: string;
}
