using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using System.IO;
using Serilog;

namespace AudioWebApi.Controllers
{

	[Produces("application/json")]
	[Route("api/Audio")]
	public class AudioController : Controller
	{
		private readonly IACRCloud _cloud;
		public AudioController(IACRCloud cloud)
		{
			_cloud = cloud;
		}

		// POST api/audio
		[HttpPost]
		public IActionResult Post(IFormFile file)
		{

            if (file == null)
			{
                if(HttpContext.Request.Form.Files.Count > 0)
                {
                    file = HttpContext.Request.Form.Files[0];
                }
                else
                {
                    Log.Error("File is missing");
                    return StatusCode(StatusCodes.Status400BadRequest, "Load file, please...");
                }
				
			}

			InfoResponse response = new InfoResponse();
			MusicResponse music = new MusicResponse();

			if (!file.FileName.EndsWith(".mp3"))
			{
				Log.Error("Failed format file, file not mp3");
				return StatusCode(StatusCodes.Status400BadRequest, music);
			}

			using (Stream stream = file.OpenReadStream())
			{
				Log.Information($"Get info on file");
				response = _cloud.GetInfo(stream);
				Log.Information($"Return info on file");

				music.Title = response.Title;
				music.Artist = response.Artist;
                music.LimitExceeded = response.LimitExceeded;
				Log.Information("Write info on music");
			}

			switch (response.Status)
			{
				case ResponseStatus.OK:
					return StatusCode(StatusCodes.Status200OK, music);
				case ResponseStatus.NotFound:
					return StatusCode(StatusCodes.Status404NotFound, music);
				case ResponseStatus.FileLarge:
					return StatusCode(StatusCodes.Status413RequestEntityTooLarge, music);
				case ResponseStatus.FileSmall:
					return StatusCode(StatusCodes.Status411LengthRequired, music);
                case ResponseStatus.LimitExceeded:
                    return StatusCode(StatusCodes.Status429TooManyRequests, music);
			}

			return StatusCode(StatusCodes.Status200OK, music);
		}
	}
}
