import { IsNumber, IsString, MinLength } from 'class-validator';

//UserLoginDto with id - for payload/user login
export class UserPayloadDto {
  @IsNumber()
  id: number;

  @IsString()
  name: string;

  @IsString()
  @MinLength(6)
  password: string;
}
